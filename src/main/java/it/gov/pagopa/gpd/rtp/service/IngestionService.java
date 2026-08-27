package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import java.util.List;
import org.springframework.messaging.Message;

public interface IngestionService {

    /**
     * Ingest a batch of {@link PaymentOption} messages from GPD eventhub
     *
     * @param message batched PaymentOption messages
     */
    void ingestPaymentOptions(Message<List<String>> message);

    /**
     * Elaborate a {@link PaymentOption} message from the dead letter storage
     *
     * @param paymentOption PaymentOption message
     */
    boolean retryDeadLetterMessage(DataCaptureMessage<PaymentOptionEvent> paymentOption);
}
