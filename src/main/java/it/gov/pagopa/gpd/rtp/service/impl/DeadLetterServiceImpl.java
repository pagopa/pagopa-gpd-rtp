package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;

@Service
@Slf4j
public class DeadLetterServiceImpl implements DeadLetterService {

    private final BlobStorageClient blobStorageClient;

    DeadLetterServiceImpl(BlobStorageClient blobStorageClient) {
        this.blobStorageClient = blobStorageClient;
    }

    @Override
    public void sendToDeadLetter(ErrorMessage errorMessage) {
        LocalDateTime now = LocalDateTime.now();
        AppException appException = (AppException) errorMessage.getPayload().getCause();

        Message<String> originalMessage = (Message<String>) errorMessage.getOriginalMessage();

        String messageId;
        String originalMessagePayload;
        try {
            messageId = new JSONObject(new String((byte[]) originalMessage.getHeaders().get(KafkaHeaders.RECEIVED_KEY))).getString("id");
        } catch (Exception e) {
            messageId = errorMessage.getHeaders().getId().toString();
        }
        try {
            originalMessagePayload = new String((byte[]) errorMessage.getOriginalMessage().getPayload());
        } catch (Exception e) {
            originalMessagePayload = "\"[ERROR] Retrieving original message payload\"";
        }
        String filePath = String.format("%s/%s/%s/%s/%s/%s_%s",
                now.getYear(),
                now.getMonthValue(),
                now.getDayOfMonth(),
                now.getHour(),
                messageId,
                appException.getAppErrorCode(),
                Instant.now());

        String stringJSON = String.format(
                "{\"id\":%s, \"cause\":\"%s\", \"errorCode\":\"%s\", \"originalMessage\":%s}",
                messageId,
                appException.getMessage(),
                appException.getAppErrorCode(),
                originalMessagePayload);
        this.blobStorageClient.saveStringJsonToBlobStorage(stringJSON, filePath);
    }
}
