package it.gov.pagopa.gpd.rtp.events.broadcast;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.pagopa.gpd.rtp.model.EventEnum;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(classes = RedisPublisher.class)
class RedisPublisherTest {

  @MockBean RedisTemplate<String, Object> redisTemplate;

  @Autowired @InjectMocks RedisPublisher redisPublisher;

  @Test
  void publishEvent() {
    when(redisTemplate.opsForStream()).thenThrow(new RuntimeException());

    Map<String, EventEnum> map = Map.of("key", EventEnum.START_CONSUMER);
    try {
      redisPublisher.publishEvent(map);
    } catch (Exception e) {
      assertTrue(e instanceof RuntimeException);
    }
  }
}
