package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import org.springframework.messaging.Message;

import java.util.List;

public interface IngestionService {


    /**
     * Ingest a {@link PaymentOption} message
     * from GPD eventhub
     *
     * @param messages PaymentOption messages
     */
    void ingestPaymentOptions(Message<List<String>> messages);

}
