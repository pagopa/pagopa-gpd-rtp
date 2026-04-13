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
import it.gov.pagopa.gpd.rtp.model.helpdesk.RetryDeadLetterEnum;
import it.gov.pagopa.gpd.rtp.model.helpdesk.RetryDeadLetterResponse;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.gpd.rtp.util.CommonUtility.getLocalDateTimeFromLong;
import static it.gov.pagopa.gpd.rtp.util.MDCUtility.*;
import static org.springframework.messaging.simp.stomp.StompHeaders.MESSAGE_ID;

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
    public RetryDeadLetterResponse retryMessages(List<String> fileNames, int minutesOffset) {
        Map<RetryDeadLetterEnum, List<String>> retryOutcomes = Map.of(
                RetryDeadLetterEnum.RETRY_SUCCESSFUL, new ArrayList<>(),
                RetryDeadLetterEnum.RETRY_DISCARDED, new ArrayList<>(),
                RetryDeadLetterEnum.RETRY_POSTPONED, new ArrayList<>(),
                RetryDeadLetterEnum.RETRY_FAILED, new ArrayList<>()
        );

        for (String fileName : fileNames) {
            RetryDeadLetterEnum outcome = retryMessage(fileName, minutesOffset);
            MDC.put(RTP_RETRY_OUTCOME, String.valueOf(outcome));

            retryOutcomes.get(outcome).add(fileName);
            log.info("Dead letter message {} retried with outcome {}", fileName, outcome);
            removeMDCDeadLetterFields();
            if (outcome.equals(RetryDeadLetterEnum.RETRY_SUCCESSFUL) || outcome.equals(RetryDeadLetterEnum.RETRY_DISCARDED)) {
                blobStorageClient.deleteBlob(fileName);
            }
        }

        return new RetryDeadLetterResponse(fileNames.size(), retryOutcomes);
    }

    private RetryDeadLetterEnum retryMessage(String fileName, int minutesOffset) {
        if (minutesOffset != 0) {
            // Verify message timestamp to ignore messages newer than the defined minutes
            try {
                Instant instant = getInstantFromFilename(fileName);
                if (instant != null && instant.isAfter(Instant.now().minus(minutesOffset, ChronoUnit.MINUTES))) {
                    return RetryDeadLetterEnum.RETRY_POSTPONED;
                }
            } catch (DateTimeParseException ignored) {
                // If date not parsable continue
            }
        }

        DataCaptureMessage<PaymentOptionEvent> paymentOption;
        try {
            paymentOption = getPaymentOptionEventDataCaptureMessage(fileName);
            setMDCPaymentOptionInfo(paymentOption);

            verifyPaymentOptionWithDB(paymentOption.getAfter());
        } catch (AppException | JsonProcessingException e) {
            return RetryDeadLetterEnum.RETRY_DISCARDED;
        }

        boolean sent = this.ingestionService.retryDeadLetterMessage(paymentOption);
        if (sent) {
            MDC.put(RTP_SENT_STATUS, "OK");
            return RetryDeadLetterEnum.RETRY_SUCCESSFUL;
        }
        MDC.put(RTP_SENT_STATUS, "KO");
        return RetryDeadLetterEnum.RETRY_FAILED;
    }

    private DataCaptureMessage<PaymentOptionEvent> getPaymentOptionEventDataCaptureMessage(String fileName) throws JsonProcessingException {
        DataCaptureMessage<PaymentOptionEvent> paymentOption;
        DeadLetterMessage deadLetterMessage =
                this.objectMapper.readValue(
                        new String(blobStorageClient.getJSONFromBlobStorage(fileName)),
                        DeadLetterMessage.class);
        MDC.put(MESSAGE_ID, String.valueOf(deadLetterMessage.getId()));

        paymentOption = deadLetterMessage.getOriginalMessage();
        if (paymentOption == null || paymentOption.getAfter() == null) {
            throw new AppException(AppError.JSON_NOT_PROCESSABLE);
        }
        return paymentOption;
    }

    private static Instant getInstantFromFilename(String fileName) {
        int instantStartIndex = fileName.lastIndexOf("_") + 1;
        int fileFormatIndex = fileName.lastIndexOf(".");
        return Instant.parse(fileName.substring(instantStartIndex, fileFormatIndex));
    }

    private void verifyPaymentOptionWithDB(PaymentOptionEvent valuesAfter) {
        PaymentOption poFromDBReplica =
                paymentOptionRepository
                        .findById(valuesAfter.getId())
                        .orElseThrow(() -> new AppException(AppError.DEAD_LETTER_MESSAGE_OUTDATED));

        LocalDateTime poMessageDate = getLocalDateTimeFromLong(valuesAfter.getLastUpdatedDate());
        if (poMessageDate.isBefore(poFromDBReplica.getLastUpdatedDate().truncatedTo(ChronoUnit.MILLIS))) {
            throw new AppException(AppError.DEAD_LETTER_MESSAGE_OUTDATED);
        }
    }


}
