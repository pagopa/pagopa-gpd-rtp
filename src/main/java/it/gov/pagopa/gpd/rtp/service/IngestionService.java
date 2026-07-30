package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import org.springframework.messaging.Message;

import java.util.List;

public interface IngestionService {

    /**
     * Ingest a batch of {@link PaymentOption} messages from GPD eventhub and sends them filtered to cdc-filtered topic
     *
     * @param messages batch of PaymentOption messages
     */
    void filterPaymentOptions(List<String> messages);

    /**
     * Ingest a {@link PaymentOption} message from filtered topic
     *
     * @param message PaymentOption message
     */
    void ingestPaymentOption(Message<String> message);

    /**
     * Elaborate a {@link PaymentOption} message from the dead letter storage
     *
     * @param paymentOption PaymentOption message
     */
    boolean retryDeadLetterMessage(DataCaptureMessage<PaymentOptionEvent> paymentOption);
}
