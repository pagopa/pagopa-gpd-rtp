package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;

public interface FilterService {

    /**
     * Evaluate if the {@link PaymentOption} message
     * is valid for RTP
     *
     * @param paymentOption PaymentOption message
     */
    void isValidPaymentOptionForRTP(DataCaptureMessage<PaymentOption> paymentOption);
}
