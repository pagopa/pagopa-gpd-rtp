package it.gov.pagopa.gpd.rtp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.rtp.events.boardcast.RedisPublisher;
import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.model.EventEnum;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Broadcast Events", description = "APIs to send events to all PODs")
public class EventsController {

  private final BindingsLifecycleController bindingsLifecycleController;
  private final ProcessingTracker processingTracker;
  private final KafkaConsumerService kafkaConsumerService;
  private final RedisPublisher redisPublisher;
  public static final String BINDING_NAME = "bindingName";

  @PostMapping("/publish/{event}")
  public ResponseEntity<String> sendBroadcaseEvent(@PathVariable EventEnum event) {
    redisPublisher.publishEvent(Map.of("event", event));
    return ResponseEntity.ok("OK");
  }
}
