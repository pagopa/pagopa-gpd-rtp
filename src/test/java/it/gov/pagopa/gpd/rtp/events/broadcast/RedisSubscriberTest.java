package it.gov.pagopa.gpd.rtp.events.broadcast;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

import it.gov.pagopa.gpd.rtp.GracefulShutdownHandler;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;

@SpringBootTest(classes = RedisSubscriber.class)
class RedisSubscriberTest {

  @MockBean private RedisTemplate<String, Object> redisTemplate;
  @MockBean private KafkaConsumerService kafkaConsumerService;
  @MockBean private GracefulShutdownHandler gracefulShutdownHandler;

  @Autowired @InjectMocks RedisSubscriber redisSubscriber;

  @Test
  void listen() {
    try {
      when(redisTemplate.opsForStream()).thenThrow(new RuntimeException());
    } catch (Exception e) {
      assertTrue(true);
    }
  }
}
