package it.gov.pagopa.gpd.rtp.events.producer.impl;

import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

import static it.gov.pagopa.gpd.rtp.util.CommonUtility.buildMessage;

@Service
@Slf4j
public class RTPMessageProducerImpl implements RTPMessageProducer {

  private final StreamBridge streamBridge;

  @Autowired
  public RTPMessageProducerImpl(StreamBridge streamBridge) {
    this.streamBridge = streamBridge;
  }

  @Override
  public boolean sendRTPMessage(RTPMessage rtpMessage) {
    var res = streamBridge.send("ingestPaymentOption-out-0", buildMessage(rtpMessage, rtpMessage.getId().toString()));

    MDC.put("topic", "rtp-events");
    MDC.put("action", "sent");
    log.debug("RTP Message Sent");
    MDC.remove("topic");
    MDC.remove("action");

    return res;
  }

  @Override
  public boolean sendFilteredCdcMessage(DataCaptureMessage<PaymentOptionEvent> filteredCdcMessage, String id) {
      return streamBridge.send("ingestCdcPaymentOption-out-0", buildMessage(filteredCdcMessage, id));
  }
}
