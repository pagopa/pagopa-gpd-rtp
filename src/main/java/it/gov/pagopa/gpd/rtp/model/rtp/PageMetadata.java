package it.gov.pagopa.gpd.rtp.model.rtp;

import lombok.*;

@Builder
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Data
public class PageMetadata {
  private Integer totalElements;
  private Integer totalPages;
  private Integer page;
  private Integer size;
}
