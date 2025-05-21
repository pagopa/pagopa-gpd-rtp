package it.gov.pagopa.gpd.rtp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentOptionStatus;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentOption {
  private int id;

  @JsonProperty("payment_position_id")
  private int paymentPositionId;

  private int amount;
  private String description;

  @JsonProperty("due_date")
  private Long dueDate;

  private String iuv;

  @JsonProperty("last_update_date")
  private Long lastUpdateDate;

  @JsonProperty("organization_fiscal_code")
  private String organizationFiscalCode;

  private PaymentOptionStatus status;

  private String nav;

  // Debtor info
  @JsonProperty("fiscal_code")
  private String fiscalCode;

  @JsonProperty("psp_code")
  private String pspCode;
  @JsonProperty("psp_tax_code")
  private String pspTaxCode;

  @JsonProperty("payment_position_status")
  private PaymentPositionStatus paymentPositionStatus;
}
