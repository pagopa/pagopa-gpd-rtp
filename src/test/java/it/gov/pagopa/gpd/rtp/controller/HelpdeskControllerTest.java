package it.gov.pagopa.gpd.rtp.controller;

import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(HelpdeskController.class)
class HelpdeskControllerTest {
    public static final String YEAR = "2025";
    public static final String MONTH = "6";
    public static final String DAY = "17";
    public static final String HOUR = "12";
    public static final String FILENAME = "testFilename.json";
    @Autowired
    private MockMvc mockMvc;
    @MockBean
    private HelpdeskService helpdeskService;

    @Test
    void getBlobList_OK() throws Exception {
        when(helpdeskService.getBlobList(YEAR, MONTH, DAY, HOUR)).thenReturn(List.of("test"));
        mockMvc
                .perform(get(String.format("/error-messages?year=%s&month=%s&day=%s&hour=%s", YEAR, MONTH, DAY, HOUR)))
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
    void retryMessage_OK() throws Exception {
        doNothing().when(helpdeskService).retryMessage(FILENAME);
        mockMvc
                .perform(post(String.format("/error-messages/retry?filename=%s", FILENAME)))
                .andExpect(status().isOk());
        verify(helpdeskService).retryMessage(FILENAME);
    }
}