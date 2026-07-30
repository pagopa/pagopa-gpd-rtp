package it.gov.pagopa.gpd.rtp.events.consumer;

import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;
import java.util.function.Consumer;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class CdcFilterConsumerConfig {

    @Bean
    public Consumer<List<String>> ingestCdcPaymentOption(IngestionService ingestionService) {
        return ingestionService::filterPaymentOptions;
    }
}
