package it.gov.pagopa.gpd.rtp.events.consumer;

import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import java.util.List;
import java.util.function.Consumer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

@Configuration
@Slf4j
@RequiredArgsConstructor
public class PaymentOptionConsumerConfig {

  @Bean
  public Consumer<Message<List<String>>> ingestPaymentOption(IngestionService ingestionService) {
    return ingestionService::ingestPaymentOptions;
  }

  @Bean
  public Consumer<ErrorMessage> deadLetterErrorHandler(DeadLetterService deadLetterService) {
    return deadLetterService::sendToDeadLetter;
  }
}
