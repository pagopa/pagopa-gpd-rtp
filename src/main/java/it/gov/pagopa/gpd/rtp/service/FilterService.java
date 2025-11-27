package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.entity.PaymentPosition;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;

import java.util.List;

public interface FilterService {


    /**
     * This method validates the tax codes in the PaymentOptionEvent.
     * In particular, it checks that the debtor's fiscal code is not the same as the organization's fiscal code.
     *
     * @param paymentOption the DataCaptureMessage containing the PaymentOptionEvent to be validated.
     *
     */
    void filterByTaxCode(
            DataCaptureMessage<PaymentOptionEvent> paymentOption);

    /**
     * This method checks if the organization associated with the PaymentOptionEvent has opted in for RTP.
     *
     * @param paymentOption the DataCaptureMessage containing the PaymentOptionEvent to be validated.
     */
    void filterByOptInFlag(
            DataCaptureMessage<PaymentOptionEvent> paymentOption);

    /**
     * This method validates the status of a PaymentPosition to ensure it is suitable for RTP processing.
     *
     * @param debtPosition the PaymentPosition to be validated.
     * @param operation the DebeziumOperationCode indicating the type of operation performed on the PaymentPosition.
     */
    void filterByStatus(
            PaymentPosition debtPosition,
            DebeziumOperationCode operation);

    /**
     * This method checks if the service type of PaymentPosition is valid for RTP processing.
     * In particular, it allows only GPD and ACA service types, excluding ACA service types with paCreatePosition.
     *
     * @param debtPosition the PaymentPosition to be validated.
     */
    void filterByServiceType(PaymentPosition debtPosition);

    /**
     * This method evaluates if the PaymentOption has valid Transfer categories for RTP processing.
     * In particular, it checks that no Transfer category starts with "6/", "7/", or "8/" and is valid.
     *
     * @param paymentOption  the PaymentOptionEvent containing the transfers to be validated.
     * @param transferList  PaymentOption's transfers
     */
    void filterByTaxonomy(PaymentOptionEvent paymentOption, List<Transfer> transferList);
}
