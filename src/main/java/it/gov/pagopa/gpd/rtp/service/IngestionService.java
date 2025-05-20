package it.gov.pagopa.gpd.rtp.service;

import org.springframework.messaging.Message;

import java.util.List;

public interface IngestionService {


    /**
     * Ingest a {@link it.gov.pagopa.gpd.rtp.events.model.entity.PaymentOption} message
     * from GPD eventhub
     *
     * @param messages PaymentOption messages
     */
    void ingestPaymentOptions(List<Message<String>> messages) throws Exception;

}
