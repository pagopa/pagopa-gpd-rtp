package it.gov.pagopa.gpd.rtp.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.gov.pagopa.gpd.rtp.events.broadcast.RedisPublisher;
import it.gov.pagopa.gpd.rtp.model.EventEnum;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(EventsController.class)
class EventsControllerTest {

  @Autowired private MockMvc mockMvc;

  @MockBean private RedisPublisher redisPublisher;

  @Test
  void sendBroadcaseEvent() throws Exception {
    mockMvc
        .perform(
            post("/events/publish/START_CONSUMER")
                .param("event", EventEnum.START_CONSUMER.toString())
                .contentType(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk());

    verify(redisPublisher).publishEvent(any());
  }
}
