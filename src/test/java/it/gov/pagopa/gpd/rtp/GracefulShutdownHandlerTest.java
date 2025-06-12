package it.gov.pagopa.gpd.rtp;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(classes = {GracefulShutdownHandler.class})
class GracefulShutdownHandlerTest {

  @MockBean private KafkaConsumerService kafkaConsumerService;
  @MockBean private ProcessingTracker processingTracker;

  @Autowired @InjectMocks GracefulShutdownHandler gracefulShutdownHandler;

  @Test
  void start() {
    gracefulShutdownHandler.start();
    assertTrue(gracefulShutdownHandler.isRunning());
  }

  @Test
  void stop() {
    gracefulShutdownHandler.stop();
    assertFalse(gracefulShutdownHandler.isRunning());
  }

  @Test
  void isRunning() {
    gracefulShutdownHandler.isRunning();
  }
}
