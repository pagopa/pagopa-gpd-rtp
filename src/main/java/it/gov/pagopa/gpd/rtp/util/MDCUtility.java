package it.gov.pagopa.gpd.rtp.util;

import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.slf4j.MDC;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class MDCUtility {
    // General keys
    public static final String START_TIME = "startTime";
    public static final String METHOD = "method";
    public static final String STATUS = "status";
    public static final String CODE = "httpCode";
    public static final String RESPONSE_TIME = "responseTime";
    public static final String FAULT_CODE = "faultCode";
    public static final String FAULT_DETAIL = "faultDetail";
    public static final String REQUEST_ID = "requestId";
    public static final String OPERATION_ID = "operationId";
    public static final String ARGS = "args";

    //RTP message keys
    public static final String NAV = "nav";
    public static final String ORGANIZATION_FISCAL_CODE = "organizationFiscalCode";
    public static final String PAYMENT_OPTION_ID = "paymentOptionId";
    public static final String MESSAGE_ID = "messageId";
    public static final String OPERATION = "operation";
    public static final String PO_STATUS = "po_status";
    public static final String PD_STATUS = "pd_status";
    public static final String RETRY_COUNT = "retryCount";
    public static final String RTP_SENT_STATUS = "rtpSentStatus";
    public static final String RTP_RETRY_OUTCOME = "rtpRetryOutcome";


    public static String getPaymentOptionId(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
        if (paymentOption.getAfter() != null) {
            return String.valueOf(paymentOption.getAfter().getId());
        }
        return String.valueOf(paymentOption.getBefore().getId());
    }

    public static String getNav(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
        if (paymentOption.getAfter() != null) {
            return String.valueOf(paymentOption.getAfter().getNav());
        }
        return String.valueOf(paymentOption.getBefore().getNav());
    }

    public static String getOrganizationFiscalCode(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
        if (paymentOption.getAfter() != null) {
            return String.valueOf(paymentOption.getAfter().getOrganizationFiscalCode());
        }
        return String.valueOf(paymentOption.getBefore().getOrganizationFiscalCode());
    }

    public static void setMDCPaymentOptionInfo(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
        MDC.put(PAYMENT_OPTION_ID, getPaymentOptionId(paymentOption));
        MDC.put(NAV, getNav(paymentOption));
        MDC.put(ORGANIZATION_FISCAL_CODE, getOrganizationFiscalCode(paymentOption));
    }

    public static void setMDCErrorField(AppException e) {
        MDC.put(FAULT_CODE, e.getAppErrorCode().name());
        MDC.put(FAULT_DETAIL, e.getMessage());
        MDC.put(RTP_SENT_STATUS, "KO");
    }

    public static void removeMDCDeadLetterFields(){
        MDC.remove(PAYMENT_OPTION_ID);
        MDC.remove(NAV);
        MDC.remove(ORGANIZATION_FISCAL_CODE);
        MDC.remove(MESSAGE_ID);
        MDC.remove(OPERATION);
        MDC.remove(PO_STATUS);
        MDC.remove(PD_STATUS);
        MDC.remove(RETRY_COUNT);
        MDC.remove(RTP_SENT_STATUS);
        MDC.remove(RTP_RETRY_OUTCOME);
    }
}
