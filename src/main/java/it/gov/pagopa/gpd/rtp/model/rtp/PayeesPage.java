package it.gov.pagopa.gpd.rtp.model.rtp;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class PayeesPage {
  private Payee[] payees;
  private PageMetadata pageMetadata;
}
