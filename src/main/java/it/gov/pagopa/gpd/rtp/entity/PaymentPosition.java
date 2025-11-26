package it.gov.pagopa.gpd.rtp.entity;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.entity.enumeration.ServiceType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.io.Serializable;

@Builder(toBuilder = true)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "payment_position")
public class PaymentPosition implements Serializable {


    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "PAYMENT_POS_SEQ")
    @SequenceGenerator(name = "PAYMENT_POS_SEQ", sequenceName = "PAYMENT_POS_SEQ", allocationSize = 1)
    private Long id;

    @NotNull
    private String iupd;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(name = "service_type")
    private ServiceType serviceType = ServiceType.GPD;


    @NotNull
    @Enumerated(EnumType.STRING)
    private PaymentPositionStatus status;
}

