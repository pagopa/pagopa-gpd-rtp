package it.gov.pagopa.gpd.rtp.events.producer.impl;

import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class RTPMessageProducerImpl implements RTPMessageProducer {

  private final StreamBridge streamBridge;

  @Autowired
  public RTPMessageProducerImpl(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  private static Message<RTPMessage> buildMessage(RTPMessage rtpMessage) {
    return MessageBuilder
            .withPayload(rtpMessage)
            .setHeader(KafkaHeaders.KEY, rtpMessage.getId().toString())
            .build();
  }

  @Override
  public boolean sendRTPMessage(RTPMessage rtpMessage) {
    var res = streamBridge.send("ingestPaymentOption-out-0", buildMessage(rtpMessage));

    MDC.put("topic", "rtp-events");
    MDC.put("action", "sent");
    log.debug("RTP Message Sent");
    MDC.remove("topic");
    MDC.remove("action");

    return res;
  }
}
