package it.gov.pagopa.gpd.rtp.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.listener.KafkaListenerErrorHandler;
import org.springframework.kafka.support.KafkaHeaders;

@Configuration
public class KafkaConfig {

    @Value("${gpd.rtp.ingestion.errorHandler.maxRetries}")
    private int maxRetries;

    @Bean
    public KafkaListenerErrorHandler errorHandler() {

        return (msg, ex) -> {
            if (msg.getHeaders().get(KafkaHeaders.DELIVERY_ATTEMPT, Integer.class) > maxRetries) {
                // TODO send to dead letter storage
                return "FAILED";
            }
            throw ex;
        };
    }
}
