package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.client.impl.RtpClientService;
import it.gov.pagopa.gpd.rtp.model.rtp.Payee;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import it.gov.pagopa.gpd.rtp.service.OptinService;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OptinServiceImpl implements OptinService {

  private final RtpClientService rtpClientService;

  private final RedisCacheRepository redisCacheRepository;
  private final KafkaConsumerService kafkaConsumerService;

  public static final int PAGE_SIZE = 100;

  @Override
  public void optInRefresh() {
    int pageNumber = 0;
    PayeesPage page;
    List<String> toCache = new ArrayList<>();
    do {
      page = rtpClientService.payees(pageNumber, PAGE_SIZE);
      List<String> flagOptIns = page.getPayees().stream().map(Payee::getPayeeId).toList();
      toCache.addAll(flagOptIns);
      pageNumber++;
    } while (page != null
        && page.getPageMetadata() != null
        && pageNumber < page.getPageMetadata().getTotalPages());
    redisCacheRepository.saveAll(toCache);
    kafkaConsumerService.startAllConsumers();
  }
}
