package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.client.RtpClient;
import it.gov.pagopa.gpd.rtp.entity.redis.FlagOptIn;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import it.gov.pagopa.gpd.rtp.repository.redis.FlagOptInRepository;
import it.gov.pagopa.gpd.rtp.service.OptinService;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OptinServiceImpl implements OptinService {

  @Autowired private RtpClient rtpClient;

  @Autowired private FlagOptInRepository flagOptInRepository;

  public static final int PAGE_SIZE = 100;

  @Override
  public void optInRefresh() {

    int pageNumber = 0;
    PayeesPage page;
    do {
      page = rtpClient.payees("v1", pageNumber, PAGE_SIZE);
      List<FlagOptIn> flagOptIns =
          Arrays.stream(page.getPayees())
              .map(elem -> FlagOptIn.builder().idEc(elem.getPayeeId()).flagValue(true).build())
              .toList();
      flagOptInRepository.saveAll(flagOptIns);
      pageNumber++;
    } while (page != null
        && page.getPageMetadata() != null
        && pageNumber < page.getPageMetadata().getTotalPages());
  }
}
