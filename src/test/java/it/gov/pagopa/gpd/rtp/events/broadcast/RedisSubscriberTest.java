package it.gov.pagopa.gpd.rtp.events.broadcast;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import it.gov.pagopa.gpd.rtp.GracefulShutdownHandler;
import it.gov.pagopa.gpd.rtp.model.EventEnum;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class RedisSubscriberTest {

    @Mock private KafkaConsumerService kafkaConsumerService;
    @Mock private GracefulShutdownHandler gracefulShutdownHandler;

    @InjectMocks private RedisSubscriber redisSubscriber;

    private static final String VERSION = "1.2.3";

    @BeforeEach
    void setUp() {
        // Manually inject the value into the @Value field
        ReflectionTestUtils.setField(redisSubscriber, "version", VERSION);
    }

    @Test
    void onMessage_ShouldStartConsumers_WhenEventIsStart() {
        Map<Object, Object> payload = Map.of(VERSION, EventEnum.START_CONSUMER.name());

        redisSubscriber.onMessage(payload);

        verify(kafkaConsumerService, times(1)).startAllConsumers();
    }

    @Test
    void onMessage_ShouldStopConsumers_WhenEventIsStop() {
        Map<Object, Object> payload = Map.of(VERSION, EventEnum.STOP_CONSUMER.name());

        redisSubscriber.onMessage(payload);

        verify(kafkaConsumerService, times(1)).stopAllConsumers();
    }

    @Test
    void onMessage_ShouldHandleListPayload() {
        // The code handles the case where the value is a list (takes the last element)
        List<String> eventList = List.of("OLD_EVENT", EventEnum.ENABLE_FORCE_KILL.name());
        Map<Object, Object> payload = Map.of(VERSION, eventList);

        redisSubscriber.onMessage(payload);

        verify(gracefulShutdownHandler, times(1)).withForceKill(true);
    }

    @Test
    void onMessage_ShouldNotFail_WhenPayloadIsNull() {
        // Verify that the internal try-catch handles nulls without crashing the test
        redisSubscriber.onMessage(null);

        verifyNoInteractions(kafkaConsumerService, gracefulShutdownHandler);
    }

    @Test
    void onMessage_ShouldHandleDisableForceKill() {
        Map<Object, Object> payload = Map.of(VERSION, EventEnum.DISABLE_FORCE_KILL.name());

        redisSubscriber.onMessage(payload);

        verify(gracefulShutdownHandler, times(1)).withForceKill(false);
    }

    @Test
    void onMessage_ShouldHandleMissingVersionKey() {
        // Payload present, but the key corresponding to the application version is missing
        Map<Object, Object> payload = Map.of("wrong-version", "START_CONSUMER");

        // Must not throw exceptions (handled by internal try-catch)
        assertDoesNotThrow(() -> redisSubscriber.onMessage(payload));

        // Must not interact with services
        verifyNoInteractions(kafkaConsumerService, gracefulShutdownHandler);
    }

    @Test
    void onMessage_ShouldHandleNullValueForVersionKey() {
        // The key exists but the value is null.
        Map<Object, Object> payload = Collections.singletonMap(VERSION, null);

        assertDoesNotThrow(() -> redisSubscriber.onMessage(payload));

        verifyNoInteractions(kafkaConsumerService, gracefulShutdownHandler);
    }

    @Test
    void onMessage_ShouldHandleEmptyList() {
        // Case: the value is an empty list
        Map<Object, Object> payload = Map.of(VERSION, List.of());

        redisSubscriber.onMessage(payload);

        verifyNoInteractions(kafkaConsumerService, gracefulShutdownHandler);
    }

    @Test
    void onMessage_ShouldIgnoreUnknownEvents() {
        // Case: string value that does not exist in EventEnum
        Map<Object, Object> payload = Map.of(VERSION, "UNKNOWN_COMMAND");

        redisSubscriber.onMessage(payload);

        verifyNoInteractions(kafkaConsumerService, gracefulShutdownHandler);
    }

    @Test
    void onMessage_ShouldHandleServiceException() {
        // Case: the command is correct, but the underlying service fails
        Map<Object, Object> payload = Map.of(VERSION, EventEnum.START_CONSUMER.name());
        doThrow(new RuntimeException("Service failure")).when(kafkaConsumerService).startAllConsumers();

        // The try-catch in Subscriber must get the error and log it, without crashing.
        assertDoesNotThrow(() -> redisSubscriber.onMessage(payload));

        verify(kafkaConsumerService).startAllConsumers();
    }
}
