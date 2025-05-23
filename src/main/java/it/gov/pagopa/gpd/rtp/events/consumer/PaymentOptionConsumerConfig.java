package it.gov.pagopa.gpd.rtp.events.consumer;

import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

import java.nio.charset.StandardCharsets;
import java.util.function.Consumer;

@Configuration
@Slf4j
public class PaymentOptionConsumerConfig {

    @Bean
    public Consumer<Message<String>> ingestPaymentOption(IngestionService ingestionService) {
        return ingestionService::ingestPaymentOption;
    }

    @Bean
    public Consumer<ErrorMessage> myErrorHandler() {
        return v -> {
            Message<String> originalmessage = (Message<String>) v.getOriginalMessage();
            Acknowledgment acknowledgment = originalmessage.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);

            Object payload = originalmessage.getPayload();

            String deserializedPayload = new String((byte[]) payload, StandardCharsets.UTF_8);
            log.debug(String.format("Dead letter message: %s%n", deserializedPayload));

            // TODO send to dead letter queue and then
            if (acknowledgment != null) {
                acknowledgment.acknowledge();
            }
        };
    }

}