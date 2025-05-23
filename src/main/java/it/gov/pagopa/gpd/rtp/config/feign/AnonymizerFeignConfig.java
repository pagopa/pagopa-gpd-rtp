package it.gov.pagopa.gpd.rtp.config.feign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnonymizerFeignConfig extends AuthFeignConfig {

  private static final String SHARED_SUBKEY_PLACEHOLDER = "${shared.subscription-key}";

  @Autowired
  public AnonymizerFeignConfig(@Value(SHARED_SUBKEY_PLACEHOLDER) String subscriptionKey) {
    this.subscriptionKey = subscriptionKey;
  }
}
