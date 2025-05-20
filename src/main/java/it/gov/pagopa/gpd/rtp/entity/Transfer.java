package it.gov.pagopa.gpd.rtp.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class Transfer {

    private int id;

    @JsonProperty("remittance_information")
    private String remittanceInformation;

    private String category;
}
