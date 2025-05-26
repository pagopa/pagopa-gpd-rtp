package it.gov.pagopa.gpd.rtp.config.feign;

import feign.RequestInterceptor;
import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static it.gov.pagopa.gpd.rtp.util.Constants.HEADER_REQUEST_ID;
import static it.gov.pagopa.gpd.rtp.util.Constants.HEADER_SUBSCRIPTION_KEY;

@Configuration
public abstract class AuthFeignConfig {

  protected String subscriptionKey;

  @Bean
  public RequestInterceptor requestIdInterceptor() {
    return requestTemplate ->
        requestTemplate
            .header(HEADER_REQUEST_ID, MDC.get("requestId"))
            .header(HEADER_SUBSCRIPTION_KEY, subscriptionKey);
  }
}
