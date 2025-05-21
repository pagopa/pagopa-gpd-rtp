package it.gov.pagopa.gpd.rtp.client;

import feign.FeignException;
import it.gov.pagopa.gpd.rtp.config.feign.AnonymizerFeignConfig;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "anonymizer", url = "${service.anonymizer.host}", configuration = AnonymizerFeignConfig.class)
public interface AnonymizerClient {

  @Retryable(
      maxAttemptsExpression = "${service.anonymizer.retry.maxAttempts}",
      backoff = @Backoff(delayExpression = "${service.anonymizer.retry.maxDelay}"))
  @PostMapping(
      value = "${service.anonymizer.path}",
      consumes = MediaType.APPLICATION_JSON_VALUE)
  String anonymize(@RequestBody String body); // TODO FIX with right configuration
}
