package it.gov.pagopa.gpd.rtp.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.rtp.events.boardcast.RedisPublisher;
import it.gov.pagopa.gpd.rtp.model.EventEnum;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Tag(name = "Broadcast Events", description = "APIs to send events to all PODs")
public class EventsController {

  @Value("${info.application.version}")
  private String version;

  private final RedisPublisher redisPublisher;

  @PostMapping("/publish/{event}")
  public ResponseEntity<String> sendBroadcaseEvent(
      @PathVariable EventEnum event,
      @RequestParam(value = "version", required = false) String specificVersion) {
    redisPublisher.publishEvent(
        Map.of(Optional.ofNullable(specificVersion).orElse(version), event));
    return ResponseEntity.ok("OK");
  }
}
