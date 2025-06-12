package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;

@SpringBootTest(classes = KafkaConsumerService.class)
class KafkaConsumerServiceTest {

  @MockBean private BindingsLifecycleController bindingsLifecycleController;
  @MockBean private ProcessingTracker processingTracker;
  @Autowired @InjectMocks KafkaConsumerService kafkaConsumerService;

  @Test
  void stopAllConsumers() {
    kafkaConsumerService.stopAllConsumers();
  }

  @Test
  void startAllConsumers() {
    kafkaConsumerService.startAllConsumers();
  }

  @Test
  void getConsumersDetails() {
    kafkaConsumerService.getConsumersDetails();
  }

  @Test
  void getStatus() {
    kafkaConsumerService.getStatus();
  }
}
