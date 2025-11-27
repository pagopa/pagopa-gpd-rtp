package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import org.springframework.messaging.Message;

public interface IngestionService {

  /**
   * Ingest a {@link PaymentOption} message from GPD eventhub
   *
   * @param message PaymentOption messages
   */
  void ingestPaymentOption(Message<String> message);

  /**
   * Elaborate a {@link PaymentOption} message from the dead letter storage
   *
   * @param paymentOption PaymentOption message
   */
  boolean retryDeadLetterMessage(DataCaptureMessage<PaymentOptionEvent> paymentOption);
}
