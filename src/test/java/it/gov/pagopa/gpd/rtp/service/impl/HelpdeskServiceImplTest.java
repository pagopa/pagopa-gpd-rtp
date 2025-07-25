package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.DeadLetterMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {HelpdeskServiceImpl.class})
class HelpdeskServiceImplTest {
    public static final String YEAR = "2025";
    public static final String MONTH = "6";
    public static final String DAY = "17";
    public static final String HOUR = "12";
    public static final String FILENAME = "testFilename.json";
    @MockBean
    private BlobStorageClient blobStorageClient;
    @MockBean
    private IngestionService ingestionService;
    @SpyBean
    private ObjectMapper objectMapper;

    @Autowired
    @InjectMocks
    private HelpdeskServiceImpl sut;

    @Test
    void getBlobList_OK() {
        List<String> blobList = List.of("test");
        when(blobStorageClient.getBlobList(YEAR, MONTH, DAY, HOUR)).thenReturn(blobList);
        List<String> response = assertDoesNotThrow(() -> sut.getBlobList(YEAR, MONTH, DAY, HOUR));
        assertTrue(blobList.containsAll(response));
        verify(blobStorageClient).getBlobList(YEAR, MONTH, DAY, HOUR);
    }

    @Test
    void getJSONFromBlobStorage_OK() {
        byte[] json = "test".getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        String response = assertDoesNotThrow(() -> sut.getJSONFromBlobStorage(FILENAME));
        assertEquals(new String(json), response);
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
    }

    @Test
    void retryMessage_OK() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage = DeadLetterMessage.builder().id("id").originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().build()).build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(ingestionService.retryDeadLetterMessage(any(DataCaptureMessage.class))).thenReturn(true);
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessage(FILENAME));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(ingestionService).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
    }

    @Test
    void retryMessage_KO_retrieve_JSON() {
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenThrow(new AppException(AppError.BLOB_STORAGE_ATTACHMENT_NOT_FOUND));
        assertThrows(AppException.class, () -> sut.retryMessage(FILENAME));
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(ingestionService, never()).retryDeadLetterMessage(any());
        verify(blobStorageClient, never()).deleteBlob(any());
    }

    @Test
    void retryMessage_KO_ingestion_message() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage = DeadLetterMessage.builder().id("id").originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().build()).build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        doThrow(new AppException(AppError.RTP_MESSAGE_NOT_SENT)).when(ingestionService).retryDeadLetterMessage(any(DataCaptureMessage.class));
        assertThrows(AppException.class, () -> sut.retryMessage(FILENAME));
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(ingestionService).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient, never()).deleteBlob(any());
    }
}
