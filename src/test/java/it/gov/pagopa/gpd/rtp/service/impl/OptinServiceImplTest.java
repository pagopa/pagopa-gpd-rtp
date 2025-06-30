package it.gov.pagopa.gpd.rtp.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import it.gov.pagopa.gpd.rtp.client.impl.RtpClientService;
import it.gov.pagopa.gpd.rtp.events.broadcast.RedisPublisher;
import it.gov.pagopa.gpd.rtp.model.rtp.PageMetadata;
import it.gov.pagopa.gpd.rtp.model.rtp.Payee;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {OptinServiceImpl.class, ObjectMapper.class})
class OptinServiceImplTest {

  @MockBean private RtpClientService rtpClientService;
  @MockBean private RedisCacheRepository redisCacheRepository;
  @MockBean private RedisPublisher redisPublisher;
  @MockBean private TelemetryClient telemetryClient;

  @Autowired @InjectMocks private OptinServiceImpl sut;
  @Captor private ArgumentCaptor<List<String>> payeesCached;

  @Test
  void optInRefresh_OK() {
    List<Payee> payeeList =
        List.of(
            Payee.builder().payeeId("payeeId1").name("payeeName1").build(),
            Payee.builder().payeeId("payeeId2").name("payeeName2").build());
    PageMetadata pageMetadata =
        PageMetadata.builder()
            .totalPages(1)
            .page(0)
            .totalElements(payeeList.size())
            .size(payeeList.size())
            .build();
    PayeesPage page = PayeesPage.builder().payees(payeeList).pageMetadata(pageMetadata).build();
    when(rtpClientService.payees(anyInt(), anyInt())).thenReturn(page);

    assertDoesNotThrow(() -> sut.optInRefresh());
    verify(rtpClientService).payees(anyInt(), anyInt());
    verify(telemetryClient, never()).trackEvent(anyString(), any(), eq(null));
    verify(redisCacheRepository).saveAll(payeesCached.capture());
    List<String> listCached = payeesCached.getValue();
    assertTrue(payeeList.stream().allMatch(el -> listCached.contains(el.getPayeeId())));
  }

  @Test
  void optInRefresh_KO() {
    List<Payee> payeeList =
        List.of(
            Payee.builder().payeeId("payeeId1").name("payeeName1").build(),
            Payee.builder().payeeId("payeeId2").name("payeeName2").build());
    PageMetadata pageMetadata =
        PageMetadata.builder()
            .totalPages(1)
            .page(0)
            .totalElements(payeeList.size())
            .size(payeeList.size())
            .build();
    PayeesPage page = PayeesPage.builder().payees(payeeList).pageMetadata(pageMetadata).build();
    when(rtpClientService.payees(anyInt(), anyInt())).thenReturn(page);
    doThrow(new RuntimeException("error")).when(redisCacheRepository).saveAll(any());

    assertThrows(RuntimeException.class, () -> sut.optInRefresh());

    verify(rtpClientService).payees(anyInt(), anyInt());
    verify(telemetryClient).trackEvent(anyString(), any(), eq(null));
  }
}
