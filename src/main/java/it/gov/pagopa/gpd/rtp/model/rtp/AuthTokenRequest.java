package it.gov.pagopa.gpd.rtp.model.rtp;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class AuthTokenRequest {

  @JsonProperty("grant_type")
  private String grantType;

  @JsonProperty("client_id")
  private String clientId;

  @JsonProperty("client_secret")
  private String clientSecret;
}
