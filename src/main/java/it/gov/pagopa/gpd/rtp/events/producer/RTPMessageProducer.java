package it.gov.pagopa.gpd.rtp.events.producer;

import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import org.springframework.stereotype.Service;

/**
 * Interface to use when required to execute sending of a {@link RTPMessage} message through
 * the eventhub channel
 */
@Service
public interface RTPMessageProducer {

    /**
     * Send a mapped {@link RTPMessage} to RTP eventhub
     *
     * @param rtpMessage data to send
     * @return boolean referring if the insertion on the sending channel was successfully
     */
    boolean sendRTPMessage(RTPMessage rtpMessage);

}