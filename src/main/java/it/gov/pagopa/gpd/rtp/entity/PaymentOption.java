package it.gov.pagopa.gpd.rtp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentOptionStatus;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Entity
@Table(name = "payment_option")
public class PaymentOption {
    @Id
    private int id;

    @JsonProperty("payment_position_id")
    @Column(name = "payment_position_id")
    private int paymentPositionId;

    private int amount;
    private String description;

    @JsonProperty("due_date")
    @Column(name = "due_date")
    private Long dueDate;

    private String iuv;

    @JsonProperty("last_update_date")
    @Column(name = "last_update_date")
    private Long lastUpdateDate;

    @JsonProperty("organization_fiscal_code")
    @Column(name = "organization_fiscal_code")
    private String organizationFiscalCode;

    private PaymentOptionStatus status;

    private String nav;

    @JsonProperty("fiscal_code")
    @Column(name = "fiscal_code")
    private String fiscalCode;

    @JsonProperty("psp_code")
    @Column(name = "psp_code")
    private String pspCode;
    @JsonProperty("psp_tax_code")
    @Column(name = "psp_tax_code")
    private String pspTaxCode;

    @JsonProperty("payment_position_status")
    @Column(name = "payment_position_status")
    private PaymentPositionStatus paymentPositionStatus;
}
