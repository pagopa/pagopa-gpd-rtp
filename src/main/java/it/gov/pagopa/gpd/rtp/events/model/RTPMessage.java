package it.gov.pagopa.gpd.rtp.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.rtp.events.model.entity.enumeration.PaymentPositionStatus;
import lombok.Builder;

import java.util.Date;

@Builder
public class RTPMessage {

    private int id;
    private String operation;
    private Long timestamp;
    private String iuv;
    private String subject;
    private String description;
    @JsonProperty("ex_tax_code")
    private String ecTaxCode;
    @JsonProperty("debtor_tax_code")
    private String debtorTaxCode;
    private String nav;
    @JsonProperty("due_date")
    private Long dueDate;
    private int amount;
    private PaymentPositionStatus status;
    @JsonProperty("psp_code")
    private String pspCode;
    @JsonProperty("psp_tax_code")
    private String pspTaxCode;
}
