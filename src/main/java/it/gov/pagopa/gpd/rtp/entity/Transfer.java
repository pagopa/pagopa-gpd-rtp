package it.gov.pagopa.gpd.rtp.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "transfer")
public class Transfer {
    @Id
    private Long id;

    @Column(name = "remittance_information")
    private String remittanceInformation;

    private String category;

    @Column(name = "payment_option_id")
    private int paymentOptionId;

    @Column(name = "organization_fiscal_code")
    private String organizationFiscalCode;

    private long amount;
}
