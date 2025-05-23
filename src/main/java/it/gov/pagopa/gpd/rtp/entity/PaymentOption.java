package it.gov.pagopa.gpd.rtp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "payment_option")
public class PaymentOption {
    @Id
    private Long id;

    @JsonProperty("payment_position_id")
    @Column(name = "payment_position_id")
    private int paymentPositionId;

    private long amount;
    private String description;

    @JsonProperty("due_date")
    @Column(name = "due_date")
    private LocalDateTime dueDate;

    private String iuv;

    @JsonProperty("last_updated_date")
    @Column(name = "last_updated_date")
    private LocalDateTime lastUpdatedDate;

    @JsonProperty("organization_fiscal_code")
    @Column(name = "organization_fiscal_code")
    private String organizationFiscalCode;

    private String status;

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

    @Enumerated(EnumType.STRING)
    @JsonProperty("payment_position_status")
    @Column(name = "payment_position_status")
    private PaymentPositionStatus paymentPositionStatus;

    @Column(name = "is_partial_payment")
    private Boolean isPartialPayment;

    @Column(name = "retention_date")
    private LocalDateTime retentionDate;

    @Column(name = "payment_date")
    private LocalDateTime paymentDate;

    @Column(name = "reporting_date")
    private LocalDateTime reportingDate;

    @Column(name = "inserted_date")
    private LocalDateTime insertedDate;

    @Column(name = "payment_method")
    private String paymentMethod;

    private long fee;

    @Column(name = "notification_fee")
    private long notificationFee;

    @Column(name = "psp_company")
    private String pspCompany;

    @Column(name = "receipt_id")
    private String idReceipt;

    @Column(name = "flow_reporting_id")
    private String idFlowReporting;

    @Column(name = "last_updated_date_notification_fee")
    private LocalDateTime lastUpdatedDateNotificationFee;

    @Column(name = "type")
    private String debtorType;

    @Column(name = "full_name")
    
    private String fullName;

    @Column(name = "street_name")
    
    private String streetName;

    @Column(name = "civic_number")
    
    private String civicNumber;

    @Column(name = "postal_code")
    
    private String postalCode;

    private String city;
    private String province;
    private String region;
    private String country;
    private String email;
    private String phone;
}
