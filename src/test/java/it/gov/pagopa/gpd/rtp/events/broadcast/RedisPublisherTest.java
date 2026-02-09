package it.gov.pagopa.gpd.rtp.events.broadcast;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import it.gov.pagopa.gpd.rtp.model.EventEnum;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;

@ExtendWith(MockitoExtension.class)
class RedisPublisherTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private RedisPublisher redisPublisher;

    @Test
    void publishEvent_ShouldSucceed() {
        // Mock client numbers
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(1L);

        Map<String, EventEnum> payload = Map.of("key", EventEnum.START_CONSUMER);

        // Verify that no exceptions are thrown
        assertDoesNotThrow(() -> redisPublisher.publishEvent(payload));

        // Verify that the template has actually been called
        verify(redisTemplate).execute(any(RedisCallback.class));
    }

    @Test
    void publishEvent_ShouldThrowException_WhenRedisFails() {
        // Mock the failure of the execute method
        when(redisTemplate.execute(any(RedisCallback.class)))
                .thenThrow(new RuntimeException("Redis connection error"));

        Map<String, EventEnum> payload = Map.of("key", EventEnum.START_CONSUMER);

        // Verify that exception is thrown
        assertThrows(RuntimeException.class, () -> redisPublisher.publishEvent(payload));
    }
}