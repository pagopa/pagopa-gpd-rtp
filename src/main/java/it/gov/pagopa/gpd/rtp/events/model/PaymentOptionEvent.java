package it.gov.pagopa.gpd.rtp.events.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.entity.enumeration.ServiceType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentOptionEvent {
  @Id private Long id;

  @JsonProperty("payment_position_id")
  private int paymentPositionId;

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

  @JsonProperty("service_type")
  private ServiceType serviceType;

  @JsonProperty("payment_plan_id")
  private String paymentPlanId;

  private String nav;

  @JsonProperty("fiscal_code")
  private String fiscalCode;

  @JsonProperty("psp_code")
  private String pspCode;

  @JsonProperty("psp_tax_code")
  private String pspTaxCode;

  @JsonProperty("payment_position_status")
  private PaymentPositionStatus paymentPositionStatus;

  @JsonProperty("is_partial_payment")
  private Boolean isPartialPayment;
}
