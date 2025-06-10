package it.gov.pagopa.gpd.rtp.model.rtp;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class Payee {
  private String payeeId;
  private String name;
}
