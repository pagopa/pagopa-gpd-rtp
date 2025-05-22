package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;

public interface FilterService {

    /**
     * Evaluate if the {@link PaymentOption} message
     * is valid for RTP
     *
     * @param paymentOption PaymentOption message
     */
    void isValidPaymentOptionForRTPOrElseThrow(DataCaptureMessage<PaymentOptionEvent> paymentOption);
}
