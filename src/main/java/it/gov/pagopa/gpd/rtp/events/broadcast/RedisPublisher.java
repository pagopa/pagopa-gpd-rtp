package it.gov.pagopa.gpd.rtp.events.broadcast;

import static it.gov.pagopa.gpd.rtp.util.Constants.STREAM_KEY;

import it.gov.pagopa.gpd.rtp.model.EventEnum;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RedisPublisher {

    private final RedisTemplate<String, Object> redisTemplate;

    public void publishEvent(Map<String, EventEnum> eventPayload) {
        Long clientsNumber = redisTemplate.execute((RedisCallback<Long>) connection ->
                connection.publish(
                        new StringRedisSerializer().serialize(STREAM_KEY),
                        new GenericJackson2JsonRedisSerializer().serialize(eventPayload)
                )
        );

        log.info("PRODUCER: message broadcasted to topic '{}' with payload: {}. \n" +
                "The number of clients that received the message is {}.", STREAM_KEY, eventPayload, clientsNumber);
    }
}