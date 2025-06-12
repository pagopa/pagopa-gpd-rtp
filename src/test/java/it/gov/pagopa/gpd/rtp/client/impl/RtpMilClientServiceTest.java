package it.gov.pagopa.gpd.rtp.client.impl;

import it.gov.pagopa.gpd.rtp.model.rtp.TokenResponse;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {RtpMilClientService.class})
class RtpMilClientServiceTest {
    @Value("${service.rtp-mil.host}")
    private String host;

    public static final String PATH = "/auth/token";

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String TOKEN_TYPE = "tokenType";
    @MockBean
    private RestTemplate restTemplate;
    @Autowired
    @InjectMocks
    private RtpMilClientService sut;

    @Test
    void getToken_OK() {
        TokenResponse tokenResponse = TokenResponse.builder().accessToken(ACCESS_TOKEN).tokenType(TOKEN_TYPE).expiresIn(1).build();
        ResponseEntity<TokenResponse> response = ResponseEntity.of(Optional.of(tokenResponse));
        when(restTemplate.postForEntity(eq(host + PATH), any(HttpEntity.class), eq(TokenResponse.class))).thenReturn(response);

        String token = assertDoesNotThrow(() -> sut.getToken());
        assertEquals(tokenResponse.getAccessToken(), token);
        verify(restTemplate).postForEntity(eq(host + PATH), any(HttpEntity.class), eq(TokenResponse.class));
    }
}