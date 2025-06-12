package it.gov.pagopa.gpd.rtp.client.impl;

import it.gov.pagopa.gpd.rtp.model.rtp.TokenResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestOperations;
import org.springframework.web.client.RestTemplate;

@Service
public class RtpMilClientService {

    @Value("${service.rtp-mil.host}")
    private String host;

    @Value("${service.rtp-mil.clientId}")
    private String clientId;

    @Value("${service.rtp-mil.clientSecret}")
    private String clientSecret;

    private final RestOperations restTemplate;

    RtpMilClientService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public static final String PATH = "/auth/token";

    public String getToken() {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> map = new LinkedMultiValueMap<>();
        map.add("grant_type", "client_credentials");
        map.add("client_id", clientId);
        map.add("client_secret", clientSecret);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(map, headers);

        ResponseEntity<TokenResponse> response =
                restTemplate.postForEntity(host + PATH, request, TokenResponse.class);

        assert response.getBody() != null;
        return response.getBody().getAccessToken();
    }
}
