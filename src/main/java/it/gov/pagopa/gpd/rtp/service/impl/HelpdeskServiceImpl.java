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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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
        List<String> nonRetriable = new ArrayList<>();

        for (String fileName : fileNames) {
            try {
                retryMessage(fileName);
            } catch (AppException ex) {
                if (AppError.RTP_MESSAGE_NOT_SENT.equals(ex.getAppErrorCode())) {
                    retriable.add(fileName);
                } else {
                    nonRetriable.add(fileName);
                    blobStorageClient.deleteBlob(fileName);
                }
            } catch (Exception ex) {
                nonRetriable.add(fileName);
                blobStorageClient.deleteBlob(fileName);
            }
        }

        if (retriable.isEmpty() && nonRetriable.isEmpty()) {
            return "Retry successful, all messages have been sent to RTP eventhub";
        }
        if((retriable.size() + nonRetriable.size()) == fileNames.size()){
            throw new AppException(AppError.RETRY_DEAD_LETTER_UNSUCCESSFUL, retriable, nonRetriable);
        }

        return String.format(
                "Retry partially successful: messages that failed and can be retried %s; messages that have been deleted and cannot be retried %s",
                retriable,
                nonRetriable
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

        // Verify if paymentOption is present on DB replica
        Long paymentOptionId = paymentOption.getAfter().getId();
        Optional<PaymentOption> poFromDBReplica = paymentOptionRepository.findById(paymentOptionId);
        if (poFromDBReplica.isEmpty()) {
            throw new AppException(AppError.PAYMENT_OPTION_NOT_FOUND);
        }

        boolean sent =
                this.ingestionService.retryDeadLetterMessage(paymentOption);

        if (sent) {
            this.blobStorageClient.deleteBlob(fileName);
            return;
        }

        throw new AppException(AppError.RTP_MESSAGE_NOT_SENT);
    }
}
