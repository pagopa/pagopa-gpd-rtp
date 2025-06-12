package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.impl.RtpClientService;
import it.gov.pagopa.gpd.rtp.model.rtp.PageMetadata;
import it.gov.pagopa.gpd.rtp.model.rtp.Payee;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {OptinServiceImpl.class, ObjectMapper.class})
class OptinServiceImplTest {

    @MockBean
    private RtpClientService rtpClientService;
    @MockBean
    private RedisCacheRepository redisCacheRepository;
    @Autowired
    @InjectMocks
    private OptinServiceImpl sut;
    @Captor
    private ArgumentCaptor<List<String>> payeesCached;

    @Test
    void optInRefresh_OK() {
        List<Payee> payeeList = List.of(Payee.builder().payeeId("payeeId1").name("payeeName1").build(), Payee.builder().payeeId("payeeId2").name("payeeName2").build());
        PageMetadata pageMetadata = PageMetadata.builder().totalPages(1).page(0).totalElements(payeeList.size()).size(payeeList.size()).build();
        PayeesPage page = PayeesPage.builder().payees(payeeList).pageMetadata(pageMetadata).build();
        when(rtpClientService.payees(anyInt(), anyInt())).thenReturn(page);

        assertDoesNotThrow(() -> sut.optInRefresh());
        verify(rtpClientService).payees(anyInt(), anyInt());
        verify(redisCacheRepository).saveAll(payeesCached.capture());
        List<String> listCached = payeesCached.getValue();
        assertTrue(payeeList.stream().allMatch(el -> listCached.contains(el.getPayeeId())));
    }
}