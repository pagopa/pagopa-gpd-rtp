package it.gov.pagopa.gpd.rtp.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.*;

@Getter
@Entity
@Table(name = "transfer")
public class Transfer {
    @Id
    private int id;

    @JsonProperty("remittance_information")
    @Column(name = "remittance_information")
    private String remittanceInformation;

    private String category;

    @JsonProperty("payment_option_id")
    @Column(name = "payment_option_id")
    private int paymentOptionId;
}
