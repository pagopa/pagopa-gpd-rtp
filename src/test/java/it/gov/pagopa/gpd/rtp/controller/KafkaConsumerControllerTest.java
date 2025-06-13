package it.gov.pagopa.gpd.rtp.controller;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(KafkaConsumerController.class)
class KafkaConsumerControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private KafkaConsumerService kafkaConsumerService;

  @Test
  void stopAllConsumers() throws Exception {
    mockMvc
        .perform(post("/kafka/consumers/stop").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(kafkaConsumerService).stopAllConsumers();
  }

  @Test
  void startAllConsumers() throws Exception {
    mockMvc
        .perform(post("/kafka/consumers/start").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    verify(kafkaConsumerService).startAllConsumers();
  }

  @Test
  void getConsumersDetails() throws Exception {
    mockMvc
        .perform(get("/kafka/consumers/").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    verify(kafkaConsumerService).getConsumersDetails();
  }

  @Test
  void getStatus() throws Exception {
    mockMvc
        .perform(get("/kafka/consumers/status").contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());
    verify(kafkaConsumerService).getStatus();
  }
}
