package it.gov.pagopa.gpd.rtp.model.rtp;

import java.util.List;
import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class PayeesPage {
  private List<Payee> payees;
  private PageMetadata pageMetadata;
}
