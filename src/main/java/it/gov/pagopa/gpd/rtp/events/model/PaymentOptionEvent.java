package it.gov.pagopa.gpd.rtp.events.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Id;
import lombok.*;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentOptionEvent {
  @Id private Long id;

  @JsonProperty("payment_position_id")
  private Long paymentPositionId;

  private long amount;
  private String description;

  @JsonProperty("due_date")
  private long dueDate;

  private String iuv;

  @JsonProperty("last_updated_date")
  private Long lastUpdatedDate;

  @JsonProperty("organization_fiscal_code")
  private String organizationFiscalCode;

  private String status;

  @JsonProperty("payment_plan_id")
  private String paymentPlanId;

  private String nav;

  @JsonProperty("fiscal_code")
  private String fiscalCode;

  @JsonProperty("psp_code")
  private String pspCode;

  @JsonProperty("psp_tax_code")
  private String pspTaxCode;

  @JsonProperty("is_partial_payment")
  private Boolean isPartialPayment;
}
