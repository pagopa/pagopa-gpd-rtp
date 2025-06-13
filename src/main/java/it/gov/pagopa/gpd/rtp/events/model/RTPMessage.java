package it.gov.pagopa.gpd.rtp.events.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.RTPOperationCode;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class RTPMessage {

  private Long id;
  private RTPOperationCode operation;
  private Long timestamp;
  private String iuv;
  private String subject;
  private String description;

  @JsonProperty("ec_tax_code")
  private String ecTaxCode;

  @JsonProperty("debtor_tax_code")
  private String debtorTaxCode;

  private String nav;

  @JsonProperty("due_date")
  private Long dueDate;

  private long amount;
  private PaymentPositionStatus status;

  @JsonProperty("psp_code")
  private String pspCode;

  @JsonProperty("psp_tax_code")
  private String pspTaxCode;

  @JsonProperty("is_partial_payment")
  private Boolean isPartialPayment;
}
