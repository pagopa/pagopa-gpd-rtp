package it.gov.pagopa.gpd.rtp.service;

import org.springframework.messaging.support.ErrorMessage;

public interface DeadLetterService {
    void sendToDeadLetter(ErrorMessage message);
}
