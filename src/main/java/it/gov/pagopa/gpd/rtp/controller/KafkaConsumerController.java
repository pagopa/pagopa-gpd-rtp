package it.gov.pagopa.gpd.rtp.controller;

import it.gov.pagopa.gpd.rtp.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/kafka/consumers")
@RequiredArgsConstructor
public class KafkaConsumerController {

  private final BindingsLifecycleController bindingsLifecycleController;
  private final ProcessingTracker processingTracker;
  private final KafkaConsumerService kafkaConsumerService;
  public static final String BINDING_NAME = "bindingName";

  @PostMapping("/stop")
  public ResponseEntity<String> stopAllConsumers() {
    kafkaConsumerService.stopAllConsumers();
    return ResponseEntity.ok("All Kafka consumers are stopped.");
  }

  @PostMapping("/start")
  public ResponseEntity<String> startAllConsumers() {
    kafkaConsumerService.startAllConsumers();
    return ResponseEntity.ok("All Kafka consumers are started.");
  }

  @GetMapping("/")
  public ResponseEntity<Object> getConsumersDetails() {
    return ResponseEntity.ok(kafkaConsumerService.getConsumersDetails());
  }

  @GetMapping("/status")
  public ResponseEntity<Map<String, Object>> getStatus() {
    return ResponseEntity.ok(kafkaConsumerService.getStatus());
  }
}
