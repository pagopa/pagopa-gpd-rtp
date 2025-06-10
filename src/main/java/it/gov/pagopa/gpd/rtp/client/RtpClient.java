package it.gov.pagopa.gpd.rtp.client;

import com.azure.core.annotation.QueryParam;
import it.gov.pagopa.gpd.rtp.config.feign.RtpFeignConfig;
import it.gov.pagopa.gpd.rtp.model.rtp.PayeesPage;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.web.bind.annotation.GetMapping;

@FeignClient(value = "rtp", url = "${service.rtp.host}", configuration = RtpFeignConfig.class)
public interface RtpClient {

  @Retryable(
      maxAttemptsExpression = "${service.rtp.retry.maxAttempts}",
      backoff = @Backoff(delayExpression = "${service.rtp.retry.maxDelay}"))
  @GetMapping(value = "${service.rtp.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
  PayeesPage payees(@QueryParam("Version") String version, Integer page, Integer size);
}
