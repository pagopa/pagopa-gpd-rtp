package it.gov.pagopa.gpd.rtp.events.boardcast;

import static it.gov.pagopa.gpd.rtp.util.Constants.STREAM_KEY;

import it.gov.pagopa.gpd.rtp.model.EventEnum;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

  private final RedisTemplate<String, Object> redisTemplate;

  public void publishEvent(Map<String, EventEnum> eventPayload) {
    ObjectRecord<String, Map<String, EventEnum>> action =
        StreamRecords.newRecord().in(STREAM_KEY).ofObject(eventPayload);

    redisTemplate.opsForStream().add(action);

    log.info("PRODUCER: message sent to '{}' with payload: {}", STREAM_KEY, eventPayload);
  }
}
