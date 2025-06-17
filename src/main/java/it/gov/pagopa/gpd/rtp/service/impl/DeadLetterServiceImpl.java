package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.gpd.rtp.util.Constants.LOG_PREFIX;

@Service
@Slf4j
@RequiredArgsConstructor
public class DeadLetterServiceImpl implements DeadLetterService {

  private final BlobStorageClient blobStorageClient;
  private final ProcessingTracker processingTracker;

  @Override
  public void sendToDeadLetter(ErrorMessage errorMessage) {
    try {
      processingTracker.messageProcessingStarted();
      handleErrorMessage(errorMessage);
    } finally {
      processingTracker.messageProcessingFinished();
    }
  }

  private void handleErrorMessage(ErrorMessage errorMessage) {
    LocalDateTime now = LocalDateTime.now();
    AppException appException = (AppException) errorMessage.getPayload().getCause();

    String messageId = getMessageId(errorMessage);
    String originalMessagePayload = getOriginalMessagePayload(errorMessage);

    String filePath =
        String.format(
            "%s/%s/%s/%s/%s/%s_%s",
            now.getYear(),
            now.getMonthValue(),
            now.getDayOfMonth(),
            now.getHour(),
            messageId,
            appException.getAppErrorCode(),
            Instant.now());

    String stringJSON =
        String.format(
            "{\"id\":%s, \"cause\":\"%s\", \"errorCode\":\"%s\", \"originalMessage\":%s}",
            messageId,
            appException.getMessage(),
            appException.getAppErrorCode(),
            originalMessagePayload);

    this.blobStorageClient.saveStringJsonToBlobStorage(stringJSON, filePath);
    log.error(
            "{} Message sent to dead letter container at {}", LOG_PREFIX, LocalDateTime.now());
  }

  private String getOriginalMessagePayload(ErrorMessage errorMessage) {
    String originalMessagePayload = "\"[ERROR] Retrieving original message payload\"";
    Message<?> originalMessage = errorMessage.getOriginalMessage();
    if (originalMessage != null) {
      try {
        originalMessagePayload = new String((byte[]) originalMessage.getPayload());
      } catch (Exception ignored) {
        // handled after
      }
    }
    return originalMessagePayload;
  }

  private String getMessageId(ErrorMessage errorMessage) {
    String messageId = String.valueOf(errorMessage.getHeaders().getId());
    Message<?> originalMessage = errorMessage.getOriginalMessage();
    if (originalMessage != null) {
      Object cdcMessageKey = originalMessage.getHeaders().get(KafkaHeaders.RECEIVED_KEY);
      if (cdcMessageKey != null) {
        try {
          messageId = new JSONObject(new String((byte[]) cdcMessageKey)).getString("id");
        } catch (Exception ignored) {
          // handled after
        }
      }
    }
    return messageId;
  }
}
