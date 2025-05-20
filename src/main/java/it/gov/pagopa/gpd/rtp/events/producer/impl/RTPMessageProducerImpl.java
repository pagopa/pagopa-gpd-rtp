package it.gov.pagopa.gpd.rtp.events.producer.impl;

import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import it.gov.pagopa.gpd.rtp.events.model.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import java.util.function.Supplier;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
@Slf4j
public class RTPMessageProducerImpl implements RTPMessageProducer {

  private final StreamBridge streamBridge;

  @Autowired
  public RTPMessageProducerImpl(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  private static Message<RTPMessage> buildMessage(
          RTPMessage rtpMessage) {
    return MessageBuilder.withPayload(rtpMessage).build();
  }

  @Override
  public boolean sendRTPMessage(
      RTPMessage rtpMessage) {
    var res = streamBridge.send("ingestPaymentOption-out-0", buildMessage(rtpMessage));

    MDC.put("topic", "payment option");
    MDC.put("action", "sent");
    log.debug("Payment Option Retry Sent");
    MDC.remove("topic");
    MDC.remove("action");

    return res;
  }

  /** Declared just to let know Spring to connect the producer at startup */
  @Slf4j
  @Configuration
  static class IngestedPaymentOptionProducerConfig {

    @Bean
    public Supplier<Flux<Message<DataCaptureMessage<PaymentOption>>>> sendIngestedPaymentOption() {
      return Flux::empty;
    }
  }
}
