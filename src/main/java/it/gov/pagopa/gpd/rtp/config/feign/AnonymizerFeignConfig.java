package it.gov.pagopa.gpd.rtp.config.feign;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AnonymizerFeignConfig extends AuthFeignConfig {

  private static final String SHARED_SUBKEY = "${service.shared.subscription-key}";

  @Autowired
  public AnonymizerFeignConfig(@Value(SHARED_SUBKEY) String subscriptionKey) {
    this.subscriptionKey = subscriptionKey;
  }
}
