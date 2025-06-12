package it.gov.pagopa.gpd.rtp.client.impl;

import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import java.net.URI;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RtpClientService {

  @Value("${service.rtp.host}")
  private String host;

  private final RtpMilClientService rtpMilClientService;
  private final RestOperations restTemplate;

  @Autowired
  public RtpClientService(RtpMilClientService rtpMilClientService, RestTemplate restTemplate) {
    this.rtpMilClientService = rtpMilClientService;
    this.restTemplate = restTemplate;
  }

  public PayeesPage payees(int page, int size) {
    String token = rtpMilClientService.getToken();

    URI uri =
        UriComponentsBuilder.fromHttpUrl(host + "/payees/payees")
            .queryParam("page", page)
            .queryParam("size", size)
            .build()
            .encode()
            .toUri();

    HttpHeaders headers = new HttpHeaders();
    headers.set("Authorization", "Bearer " + token);
    headers.set("Version", "v1");
    headers.set("RequestId", MDC.get("requestId"));

    HttpEntity<Void> entity = new HttpEntity<>(headers);

    ResponseEntity<PayeesPage> response =
        restTemplate.exchange(uri, HttpMethod.GET, entity, PayeesPage.class);
    return response.getBody();
  }
}
