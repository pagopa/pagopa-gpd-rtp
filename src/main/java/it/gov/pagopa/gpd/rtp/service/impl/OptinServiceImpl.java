package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.client.impl.RtpClientService;
import it.gov.pagopa.gpd.rtp.entity.redis.FlagOptIn;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import it.gov.pagopa.gpd.rtp.repository.redis.FlagOptInRepository;
import it.gov.pagopa.gpd.rtp.service.OptinService;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OptinServiceImpl implements OptinService {

  private final RtpClientService rtpClientService;

  private final FlagOptInRepository flagOptInRepository;

  public static final int PAGE_SIZE = 100;

  @Autowired
  public OptinServiceImpl(
      RtpClientService rtpClientService, FlagOptInRepository flagOptInRepository) {
    this.rtpClientService = rtpClientService;
    this.flagOptInRepository = flagOptInRepository;
  }

  @Override
  public void optInRefresh() {
    int pageNumber = 0;
    PayeesPage page;
    do {
      page = rtpClientService.payees(pageNumber, PAGE_SIZE);
      List<FlagOptIn> flagOptIns =
          page.getPayees().stream()
              .map(elem -> FlagOptIn.builder().idEc(elem.getPayeeId()).flagValue(true).build())
              .toList();
      flagOptInRepository.saveAll(flagOptIns);
      pageNumber++;
    } while (page != null
        && page.getPageMetadata() != null
        && pageNumber < page.getPageMetadata().getTotalPages());
  }
}
