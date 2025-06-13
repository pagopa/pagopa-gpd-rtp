package it.gov.pagopa.gpd.rtp.controller;

import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PodController {

  @Value("${pod.name:unknown}")
  private String podName;

  private final ProcessingTracker processingTracker;

  @GetMapping("/status")
  public Map<String, Object> getStatus() {
    return Map.of("podName", podName, "inProgress", processingTracker.isProcessing());
  }
}
