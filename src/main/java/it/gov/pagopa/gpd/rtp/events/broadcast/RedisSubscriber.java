package it.gov.pagopa.gpd.rtp.events.broadcast;

import it.gov.pagopa.gpd.rtp.GracefulShutdownHandler;
import it.gov.pagopa.gpd.rtp.model.EventEnum;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;

import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class RedisSubscriber {

    private final KafkaConsumerService kafkaConsumerService;
    private final GracefulShutdownHandler gracefulShutdownHandler;

    @Value("${info.application.version}")
    private String version;

    /**
     * This method is automatically invoked by MessageListenerAdapter
     * for each message received on the channel.
     */
    public void onMessage(Map<Object, Object> payload) {
        try {
            log.info("SUBSCRIBER: message received: {}", payload);

            if (payload != null) {
                Object eventAction = payload.get(version);
                String eventActionString;

                if (eventAction instanceof List<?> list && !list.isEmpty()) {
                    eventAction = list.get(list.size() - 1);
                }

                if(eventAction == null) {
                    return;
                }

                eventActionString = eventAction.toString();

                if (EventEnum.START_CONSUMER.name().equals(eventActionString)) {
                    kafkaConsumerService.startAllConsumers();
                } else if (EventEnum.STOP_CONSUMER.name().equals(eventActionString)) {
                    kafkaConsumerService.stopAllConsumers();
                } else if (EventEnum.ENABLE_FORCE_KILL.name().equals(eventActionString)) {
                    gracefulShutdownHandler.withForceKill(true);
                } else if (EventEnum.DISABLE_FORCE_KILL.name().equals(eventActionString)) {
                    gracefulShutdownHandler.withForceKill(false);
                }
            }
        } catch (Exception e) {
            log.error("Error processing broadcast message", e);
        }
    }
}
