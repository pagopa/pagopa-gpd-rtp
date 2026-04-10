package it.gov.pagopa.gpd.rtp.controller;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import it.gov.pagopa.gpd.rtp.model.helpdesk.RetryDeadLetterResponse;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(HelpdeskController.class)
class HelpdeskControllerTest {
  public static final String YEAR = "2025";
  public static final String MONTH = "6";
  public static final String DAY = "17";
  public static final String HOUR = "12";
  public static final String FILENAME = "testFilename.json";
  public static final List<String> FILENAMES = Collections.singletonList(FILENAME);
  @Autowired private MockMvc mockMvc;
  @MockBean private HelpdeskService helpdeskService;

  @Test
  void getBlobList_OK() throws Exception {
    when(helpdeskService.getBlobList(YEAR, MONTH, DAY, HOUR)).thenReturn(List.of("test"));
    mockMvc
        .perform(
            get(
                String.format(
                    "/error-messages?year=%s&month=%s&day=%s&hour=%s", YEAR, MONTH, DAY, HOUR)))
        .andExpect(status().isOk());
    verify(helpdeskService).getBlobList(YEAR, MONTH, DAY, HOUR);
  }

  @Test
  void getJSONFromBlobStorage_OK() throws Exception {
    when(helpdeskService.getJSONFromBlobStorage(FILENAME)).thenReturn("{\"id\": 1}");
    mockMvc
        .perform(get(String.format("/error-messages/detail?filename=%s", FILENAME)))
        .andExpect(status().isOk());
    verify(helpdeskService).getJSONFromBlobStorage(FILENAME);
  }

  @Test
  void retryMessage_OK_MinutesOffset_Default() throws Exception {
    when(helpdeskService.retryMessages(eq(FILENAMES), anyInt())).thenReturn(new RetryDeadLetterResponse());
    mockMvc
        .perform(
            post("/error-messages/retry")
                .contentType(MediaType.APPLICATION_JSON)
                .content(String.format("[\"%s\"]", FILENAME)))
        .andExpect(status().isOk());
    verify(helpdeskService).retryMessages(FILENAMES, 2);
  }

  @Test
  void retryMessage_OK_MinutesOffset_Defined() throws Exception {
    when(helpdeskService.retryMessages(eq(FILENAMES), anyInt())).thenReturn(new RetryDeadLetterResponse());
    mockMvc
            .perform(
                    post("/error-messages/retry?minutesOffset=10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("[\"%s\"]", FILENAME)))
            .andExpect(status().isOk());
    verify(helpdeskService).retryMessages(FILENAMES, 10);
  }

  @Test
  void retryAllMessages_OK() throws Exception {
    when(helpdeskService.retryMessages(eq(FILENAMES), anyInt())).thenReturn(new RetryDeadLetterResponse());
    when(helpdeskService.getBlobList(null, null, null, null)).thenReturn(FILENAMES);

    mockMvc
            .perform(
                    post("/error-messages/retry/all")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("[\"%s\"]", FILENAME)))
            .andExpect(status().isOk());
    verify(helpdeskService).retryMessages(FILENAMES, 2);
    verify(helpdeskService).getBlobList(null, null, null, null);
  }

  @Test
  void retryAllMessages_OK_MinutesOffset_Defined() throws Exception {
    when(helpdeskService.retryMessages(eq(FILENAMES), anyInt())).thenReturn(new RetryDeadLetterResponse());
    when(helpdeskService.getBlobList(null, null, null, null)).thenReturn(FILENAMES);

    mockMvc
            .perform(
                    post("/error-messages/retry/all?minutesOffset=10")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(String.format("[\"%s\"]", FILENAME)))
            .andExpect(status().isOk());
    verify(helpdeskService).retryMessages(FILENAMES, 10);
    verify(helpdeskService).getBlobList(null, null, null, null);
  }
}
