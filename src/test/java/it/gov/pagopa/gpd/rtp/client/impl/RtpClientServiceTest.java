package it.gov.pagopa.gpd.rtp.client.impl;

import it.gov.pagopa.gpd.rtp.model.rtp.PageMetadata;
import it.gov.pagopa.gpd.rtp.model.rtp.Payee;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import it.gov.pagopa.gpd.rtp.model.rtp.TokenResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RtpClientService.class})
class RtpClientServiceTest {
    @Value("${service.rtp.host}")
    private String host;
    @MockBean
    private RtpMilClientService rtpMilClientService;

    @MockBean
    private RestTemplate restTemplate;
    @Autowired
    @InjectMocks
    private RtpClientService sut;

    @Test
    void payees_OK() {
        when(rtpMilClientService.getToken()).thenReturn("token");
        List<Payee> payeeList = List.of(Payee.builder().payeeId("payeeId1").name("payeeName1").build(), Payee.builder().payeeId("payeeId2").name("payeeName2").build());
        PageMetadata pageMetadata = PageMetadata.builder().totalPages(1).page(0).totalElements(payeeList.size()).size(payeeList.size()).build();
        PayeesPage page = PayeesPage.builder().payees(payeeList).pageMetadata(pageMetadata).build();

        URI uri =
                UriComponentsBuilder.fromHttpUrl(host + "/payees/payees")
                        .queryParam("page", 1)
                        .queryParam("size", payeeList.size())
                        .build()
                        .encode()
                        .toUri();
        ResponseEntity<PayeesPage> response = ResponseEntity.of(Optional.of(page));
        when(restTemplate.exchange(eq(uri), eq(HttpMethod.GET), any(HttpEntity.class), eq(PayeesPage.class))).thenReturn(response);

        assertDoesNotThrow(() -> sut.payees(1, payeeList.size()));
        verify(rtpMilClientService).getToken();
        verify(restTemplate).exchange(eq(uri), eq(HttpMethod.GET), any(HttpEntity.class), eq(PayeesPage.class));
    }
}