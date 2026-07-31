package it.gov.pagopa.gpd.rtp.utils;

import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Date;

public class EntityUtils {
    public static final LocalDateTime DATE_NOW = LocalDateTime.now();
    public static final String ORG_FISCAL_CODE = "orgFiscalCode";

    public static DataCaptureMessage<PaymentOptionEvent> getPaymentOption(
            DebeziumOperationCode debeziumOperationCode) {
        PaymentOptionEvent pp =
                PaymentOptionEvent.builder()
                        .id(10L)
                        .paymentPositionId(1L)
                        .amount(0)
                        .description("description")
                        .dueDate(new Date().getTime())
                        .iuv("iuv")
                        .lastUpdatedDate(Timestamp.valueOf(DATE_NOW).getTime() * 1000)
                        .organizationFiscalCode(ORG_FISCAL_CODE)
                        .status("PO_PAID")
                        .nav("nav")
                        .fiscalCode("fiscal_code")
                        .pspCode("pspCode")
                        .pspTaxCode("pspTaxCode")
                        .isPartialPayment(false)
                        .build();
        return DataCaptureMessage.<PaymentOptionEvent>builder()
                .before(null)
                .after(pp)
                .op(debeziumOperationCode)
                .tsMs(10L)
                .tsNs(0L)
                .tsUs(0L)
                .build();
    }
}
