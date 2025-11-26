package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.entity.PaymentPosition;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;

import java.util.List;

public interface FilterService {


    void filterByTaxCode(
            DataCaptureMessage<PaymentOptionEvent> paymentOption);

    void filterByOptInFlag(
            DataCaptureMessage<PaymentOptionEvent> paymentOption);

    void filterByStatus(
            PaymentPosition debtPosition,
            DebeziumOperationCode operation);

    void filterByServiceType(PaymentPosition debtPosition);

    /**
     * Evaluate if the {@link PaymentOption}
     * has the valid {@link Transfer} categories
     *
     * @param transferList PaymentOption's transfers
     */
    void filterByTaxonomy(PaymentOptionEvent paymentOption, List<Transfer> transferList);
}
