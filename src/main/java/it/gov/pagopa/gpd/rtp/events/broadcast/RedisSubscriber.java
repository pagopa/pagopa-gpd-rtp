package it.gov.pagopa.gpd.rtp.events.broadcast;

import static it.gov.pagopa.gpd.rtp.util.Constants.*;

import it.gov.pagopa.gpd.rtp.GracefulShutdownHandler;
import it.gov.pagopa.gpd.rtp.model.EventEnum;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import jakarta.annotation.PostConstruct;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisSubscriber {

  @Value("${info.application.version}")
  private String version;

  private final RedisTemplate<String, Object> redisTemplate;
  private final KafkaConsumerService kafkaConsumerService;
  private final GracefulShutdownHandler gracefulShutdownHandler;

  @PostConstruct
  private void startListening() {
    try {
      redisTemplate.opsForStream().createGroup(STREAM_KEY, ReadOffset.latest(), GROUP_NAME);
    } catch (Exception e) {
      log.info("Group '{}' already exists. No need to create it.", GROUP_NAME);
    }

    new Thread(this::listen).start();
  }

  void listen() {
    log.info("CONSUMER {}: start listening...", GROUP_NAME);
    while (true) {
      List<MapRecord<String, Object, Object>> messages =
          redisTemplate
              .opsForStream()
              .read(
                  Consumer.from(GROUP_NAME, CONSUMER_NAME),
                  StreamReadOptions.empty().count(1).block(Duration.ofSeconds(10)),
                  StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed()));

      if (messages != null && !messages.isEmpty()) {
        for (MapRecord<String, Object, Object> message : messages) {
          log.info(
              "CONSUMER {}: message {} received: {}",
              GROUP_NAME,
              message.getId(),
              message.getValue());

          if (message.getValue() != null && message.getValue() instanceof Map) {
            Map<Object, Object> payload = message.getValue();

            if (payload.get(version) != null
                && payload.get(version).equals(EventEnum.START_CONSUMER)) {
              kafkaConsumerService.startAllConsumers();
            }
            if (payload.get(version) != null
                && payload.get(version).equals(EventEnum.STOP_CONSUMER)) {
              kafkaConsumerService.stopAllConsumers();
            }
            if (payload.get(version) != null
                && payload.get(version).equals(EventEnum.ENABLE_FORCE_KILL)) {
              gracefulShutdownHandler.withForceKill(true);
            }
          }

          redisTemplate.opsForStream().acknowledge(GROUP_NAME, message);
        }
      }
    }
  }
}
