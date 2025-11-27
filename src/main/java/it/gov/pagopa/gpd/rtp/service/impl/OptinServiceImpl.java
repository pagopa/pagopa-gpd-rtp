package it.gov.pagopa.gpd.rtp.service.impl;

import com.microsoft.applicationinsights.TelemetryClient;
import it.gov.pagopa.gpd.rtp.client.impl.RtpClientService;
import it.gov.pagopa.gpd.rtp.events.broadcast.RedisPublisher;
import it.gov.pagopa.gpd.rtp.model.EventEnum;
import it.gov.pagopa.gpd.rtp.model.rtp.Payee;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import it.gov.pagopa.gpd.rtp.service.OptinService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static it.gov.pagopa.gpd.rtp.util.Constants.CUSTOM_EVENT;

@Service
@Slf4j
@RequiredArgsConstructor
public class OptinServiceImpl implements OptinService {

    public static final int PAGE_SIZE = 100;
    private final RtpClientService rtpClientService;
    private final RedisCacheRepository redisCacheRepository;
    private final RedisPublisher redisPublisher;
    private final TelemetryClient telemetryClient;
    @Value("${info.application.version}")
    private String version;

    @Override
    public void optInRefresh() {
        int pageNumber = 0;
        PayeesPage page;
        List<String> toCache = new ArrayList<>();

        try {
            do {
                page = this.rtpClientService.payees(pageNumber, PAGE_SIZE);
                List<String> flagOptIns = page.getPayees().stream().map(Payee::getPayeeId).toList();
                toCache.addAll(flagOptIns);
                pageNumber++;
            } while (page != null
                    && page.getPageMetadata() != null
                    && pageNumber < page.getPageMetadata().getTotalPages());

            this.redisCacheRepository.saveAll(toCache);
            this.redisPublisher.publishEvent(Map.of(version, EventEnum.START_CONSUMER));
        } catch (Exception e) {
            log.error("Unexpected error while refreshing optIn flag", e);
            Map<String, String> props =
                    Map.of(
                            "type",
                            "OPT_IN_REFRESH_ERROR",
                            "title",
                            "Unexpected error while refreshing optIn flag",
                            "details",
                            e.getMessage(),
                            "cause",
                            e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            this.telemetryClient.trackEvent(CUSTOM_EVENT, props, null);
            throw e;
        }
    }
}
