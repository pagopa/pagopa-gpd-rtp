package it.gov.pagopa.gpd.rtp.events.broadcast;

import static it.gov.pagopa.gpd.rtp.util.Constants.STREAM_KEY;

import it.gov.pagopa.gpd.rtp.model.EventEnum;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  public void publishEvent(Map<String, EventEnum> eventPayload) {
    // Publish to the channel in broadcast
    redisTemplate.convertAndSend(STREAM_KEY, eventPayload);

    log.info("PRODUCER: message broadcasted to topic '{}' with payload: {}", STREAM_KEY, eventPayload);
  }
}