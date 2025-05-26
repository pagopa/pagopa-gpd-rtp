package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.client.DeadLetterBlobStorageClient;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@Slf4j
public class DeadLetterServiceImpl implements DeadLetterService {

    private final DeadLetterBlobStorageClient deadLetterBlobStorageClient;

    DeadLetterServiceImpl(DeadLetterBlobStorageClient deadLetterBlobStorageClient){
        this.deadLetterBlobStorageClient = deadLetterBlobStorageClient;
    }

    @Override
    public void sendToDeadLetter(ErrorMessage errorMessage){
        Acknowledgment acknowledgment = errorMessage.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);

        boolean response = this.deadLetterBlobStorageClient.saveErrorMessageToBlobStorage(errorMessage, "test_id"); // TODO

        if (acknowledgment != null) {
            if(response){
                acknowledgment.acknowledge();
            } else {
                acknowledgment.nack(Duration.ofSeconds(1));
            }
        }
    }
}
