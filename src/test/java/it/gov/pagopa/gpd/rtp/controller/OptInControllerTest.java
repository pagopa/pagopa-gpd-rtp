package it.gov.pagopa.gpd.rtp.controller;

import it.gov.pagopa.gpd.rtp.service.OptinService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.verify;

@SpringBootTest(classes = {OptInController.class})
class OptInControllerTest {

    @MockBean
    private OptinService optinService;
    @Autowired
    @InjectMocks
    private OptInController sut;

    @Test
    void optInRefresh_OK(){
        assertDoesNotThrow(() -> sut.optInRefresh());
        verify(optinService).optInRefresh();
    }
}