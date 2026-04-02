package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.DeadLetterMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class HelpdeskServiceImpl implements HelpdeskService {

    private final BlobStorageClient blobStorageClient;
    private final IngestionService ingestionService;
    private final ObjectMapper objectMapper;
    private final PaymentOptionRepository paymentOptionRepository;

    @Override
    public List<String> getBlobList(String year, String month, String day, String hour) {
        return blobStorageClient.getBlobList(year, month, day, hour);
    }

    @Override
    public String getJSONFromBlobStorage(String fileName) {
        return new String(blobStorageClient.getJSONFromBlobStorage(fileName));
    }

    @Override
    public String retryMessages(List<String> fileNames) {
        List<String> retriable = new ArrayList<>();
        List<String> ignored = new ArrayList<>();

        for (String fileName : fileNames) {
            boolean deleteBlob = true;
            try {
                retryMessage(fileName);
     
            } catch (AppException ex) {
                if (AppError.RTP_MESSAGE_NOT_SENT.equals(ex.getAppErrorCode())) {
                    retriable.add(fileName);
                    deleteBlob = false;
                } else {
                    ignored.add(fileName);
                }
            } catch (JsonProcessingException ex) {
                ignored.add(fileName);
            }
            if (deleteBlob) {
                blobStorageClient.deleteBlob(fileName);
            }
        }

        if (retriable.isEmpty() && ignored.isEmpty()) {
            return "Retry successful, all messages have been sent to RTP eventhub";
        }
        if (ignored.size() == fileNames.size()) {
            return "All messages have been ignored because outdated or not processable";
        }

        return String.format(
                "Retry partially successful: this messages failed and can be retried %s",
                retriable
        );
    }

    private void retryMessage(String fileName) throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage =
                this.objectMapper.readValue(
                        new String(blobStorageClient.getJSONFromBlobStorage(fileName)),
                        DeadLetterMessage.class);

        DataCaptureMessage<PaymentOptionEvent> paymentOption = deadLetterMessage.getOriginalMessage();
        if (paymentOption == null || paymentOption.getAfter() == null) {
            throw new AppException(AppError.JSON_NOT_PROCESSABLE);
        }

        verifyPaymentOptionWithDB(paymentOption.getAfter());

        boolean sent = this.ingestionService.retryDeadLetterMessage(paymentOption);

        if (!sent) {
            throw new AppException(AppError.RTP_MESSAGE_NOT_SENT);
        }
    }

    private void verifyPaymentOptionWithDB(PaymentOptionEvent valuesAfter) {
        PaymentOption poFromDBReplica =
                paymentOptionRepository
                        .findById(valuesAfter.getId())
                        .orElseThrow(() -> new AppException(AppError.DEAD_LETTER_MESSAGE_OUTDATED));
        if (poFromDBReplica == null) {
            throw new AppException(AppError.DEAD_LETTER_MESSAGE_OUTDATED);
        }
        Instant poMessageInstant = Instant.ofEpochMilli(valuesAfter.getLastUpdatedDate() / 1000);
        LocalDateTime poMessageDate = LocalDateTime.ofInstant(poMessageInstant, ZoneOffset.UTC);
        if (poMessageDate.isBefore(poFromDBReplica.getLastUpdatedDate())) {
            throw new AppException(AppError.DEAD_LETTER_MESSAGE_OUTDATED);
        }
    }
}
