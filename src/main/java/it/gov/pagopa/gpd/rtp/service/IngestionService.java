package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import org.springframework.messaging.Message;

public interface IngestionService {


    /**
     * Ingest a {@link PaymentOption} message
     * from GPD eventhub
     *
     * @param message PaymentOption messages
     */
    void ingestPaymentOption(Message<String> message);

    /**
     * Elaborate a {@link PaymentOption} message
     *
     * @param message PaymentOption messages
     */
    void handleMessage(Message<?> message);

}
