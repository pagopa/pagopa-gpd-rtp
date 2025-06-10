package it.gov.pagopa.gpd.rtp.config.feign;

import static it.gov.pagopa.gpd.rtp.util.Constants.HEADER_REQUEST_ID;
import static it.gov.pagopa.gpd.rtp.util.Constants.HEADER_SUBSCRIPTION_KEY;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

@Component
public class RtpFeignConfig extends AuthFeignConfig {

  private static final String RTP_SUBKEY = "${service.rtp.subscription-key}";

  @Autowired
  public RtpFeignConfig(@Value(RTP_SUBKEY) String subscriptionKey) {
    this.subscriptionKey = subscriptionKey;
  }

  @Bean
  public RequestInterceptor requestIdInterceptor() {
    return requestTemplate ->
        requestTemplate
            .header(HEADER_REQUEST_ID, MDC.get("RequestId"))
            .header(HEADER_SUBSCRIPTION_KEY, subscriptionKey);
  }
}
