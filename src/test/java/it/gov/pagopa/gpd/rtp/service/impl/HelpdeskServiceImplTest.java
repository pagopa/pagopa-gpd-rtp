package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.DeadLetterMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {HelpdeskServiceImpl.class})
class HelpdeskServiceImplTest {
    public static final String YEAR = "2025";
    public static final String MONTH = "6";
    public static final String DAY = "17";
    public static final String HOUR = "12";
    public static final String FILENAME = "testFilename.json";
    public static final String FILENAME_2 = "testFilename_2.json";
    public static final long PAYMENT_OPTION_ID = 100L;
    public static final PaymentOptionEvent paymentOptionEvent1 = PaymentOptionEvent.builder().id(PAYMENT_OPTION_ID).build();
    public static final long PAYMENT_OPTION_ID_2 = 102L;
    public static final PaymentOptionEvent paymentOptionEvent2 = PaymentOptionEvent.builder().id(PAYMENT_OPTION_ID_2).build();
    public static final PaymentOption paymentOptionFromDB = new PaymentOption();

    @MockBean
    private BlobStorageClient blobStorageClient;
    @MockBean
    private IngestionService ingestionService;
    @MockBean
    private PaymentOptionRepository paymentOptionRepository;
    @SpyBean
    private ObjectMapper objectMapper;

    @Autowired
    @InjectMocks
    private HelpdeskServiceImpl sut;

    @BeforeEach
    void setup() {
        LocalDateTime dateNow = LocalDateTime.now();
        paymentOptionFromDB.setLastUpdatedDate(dateNow);
        Long dateNowEvent = Timestamp.valueOf(dateNow.plusHours(2)).getTime() * 1000;
        paymentOptionEvent1.setLastUpdatedDate(dateNowEvent);
        paymentOptionEvent2.setLastUpdatedDate(dateNowEvent);
    }

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
    void retryMessages_SingleMessage_OK() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent1).build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID)).thenReturn(Optional.of(paymentOptionFromDB));
        when(ingestionService.retryDeadLetterMessage(any(DataCaptureMessage.class))).thenReturn(true);
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessages(Collections.singletonList(FILENAME)));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(paymentOptionRepository).findById(PAYMENT_OPTION_ID);
        verify(ingestionService).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
    }

    @Test
    void retryMessages_DoubleMessage_OK() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent1).build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME_2)).thenReturn(json);
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID)).thenReturn(Optional.of(paymentOptionFromDB));
        when(ingestionService.retryDeadLetterMessage(any(DataCaptureMessage.class))).thenReturn(true);
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        when(blobStorageClient.deleteBlob(FILENAME_2)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessages(List.of(FILENAME, FILENAME_2)));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME_2);
        verify(paymentOptionRepository, times(2)).findById(PAYMENT_OPTION_ID);
        verify(ingestionService, times(2)).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
        verify(blobStorageClient).deleteBlob(FILENAME_2);
    }

    @Test
    void retryMessages_OneMessage_PaymentOptionNotFound_OneMessage_OK() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent1).build())
                        .build();
        DeadLetterMessage deadLetterMessage2 =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent2).build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        byte[] json2 = objectMapper.writeValueAsString(deadLetterMessage2).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME_2)).thenReturn(json2);
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID)).thenReturn(Optional.of(paymentOptionFromDB));
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID_2)).thenReturn(Optional.empty());
        when(ingestionService.retryDeadLetterMessage(any(DataCaptureMessage.class))).thenReturn(true);
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        when(blobStorageClient.deleteBlob(FILENAME_2)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessages(List.of(FILENAME, FILENAME_2)));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME_2);
        verify(paymentOptionRepository, times(1)).findById(PAYMENT_OPTION_ID);
        verify(paymentOptionRepository, times(1)).findById(PAYMENT_OPTION_ID_2);
        verify(ingestionService, times(1)).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
        verify(blobStorageClient).deleteBlob(FILENAME_2);
    }

    @Test
    void retryMessages_KO_retrieve_JSON() {
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME))
                .thenThrow(new AppException(AppError.BLOB_STORAGE_ATTACHMENT_NOT_FOUND));
        assertDoesNotThrow(() -> sut.retryMessages(Collections.singletonList(FILENAME)));
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(ingestionService, never()).retryDeadLetterMessage(any());
        verify(blobStorageClient).deleteBlob(any());
    }

    @Test
    void retryMessages_KO_JsonProcessingException() throws JsonProcessingException {
        paymentOptionEvent1.setLastUpdatedDate(1000L);
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessages(List.of(FILENAME)));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(paymentOptionRepository, never()).findById(PAYMENT_OPTION_ID);
        verify(ingestionService, never()).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
    }

    @Test
    void retryMessages_KO_message_outdated() throws JsonProcessingException {
        paymentOptionEvent1.setLastUpdatedDate(1000L);
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent1).build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID)).thenReturn(Optional.of(paymentOptionFromDB));
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessages(List.of(FILENAME)));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(paymentOptionRepository, times(1)).findById(PAYMENT_OPTION_ID);
        verify(ingestionService, never()).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
    }

    @Test
    void retryMessages_KO_ingestion_message() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent1).build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID)).thenReturn(Optional.of(paymentOptionFromDB));
        doThrow(new AppException(AppError.RTP_MESSAGE_NOT_SENT))
                .when(ingestionService)
                .retryDeadLetterMessage(any(DataCaptureMessage.class));
        assertDoesNotThrow(() -> sut.retryMessages(Collections.singletonList(FILENAME)));
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(paymentOptionRepository).findById(PAYMENT_OPTION_ID);
        verify(ingestionService).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient, never()).deleteBlob(any());
    }

    @Test
    void retryMessages_DoubleMessage_PaymentOptionNotFound_KO() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent1).build())
                        .build();
        DeadLetterMessage deadLetterMessage2 =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent2).build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        byte[] json2 = objectMapper.writeValueAsString(deadLetterMessage2).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME_2)).thenReturn(json2);
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID)).thenReturn(Optional.empty());
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID_2)).thenReturn(Optional.empty());
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        when(blobStorageClient.deleteBlob(FILENAME_2)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessages(List.of(FILENAME, FILENAME_2)));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME_2);
        verify(paymentOptionRepository, times(1)).findById(PAYMENT_OPTION_ID);
        verify(paymentOptionRepository, times(1)).findById(PAYMENT_OPTION_ID_2);
        verify(ingestionService, never()).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
        verify(blobStorageClient).deleteBlob(FILENAME_2);
    }

    @Test
    void retryMessages_OneMessage_PaymentOptionNotFound_OneMessage_EHSendKO() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent1).build())
                        .build();
        DeadLetterMessage deadLetterMessage2 =
                DeadLetterMessage.builder()
                        .id("id")
                        .originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().after(paymentOptionEvent2).build())
                        .build();
        byte[] json = objectMapper.writeValueAsString(deadLetterMessage).getBytes();
        byte[] json2 = objectMapper.writeValueAsString(deadLetterMessage2).getBytes();
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME)).thenReturn(json);
        when(blobStorageClient.getJSONFromBlobStorage(FILENAME_2)).thenReturn(json2);
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID)).thenReturn(Optional.empty());
        when(paymentOptionRepository.findById(PAYMENT_OPTION_ID_2)).thenReturn(Optional.of(paymentOptionFromDB));
        when(ingestionService.retryDeadLetterMessage(deadLetterMessage.getOriginalMessage())).thenReturn(true);
        when(ingestionService.retryDeadLetterMessage(deadLetterMessage2.getOriginalMessage())).thenReturn(false);
        when(blobStorageClient.deleteBlob(FILENAME)).thenReturn(true);
        assertDoesNotThrow(() -> sut.retryMessages(List.of(FILENAME, FILENAME_2)));

        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME);
        verify(blobStorageClient).getJSONFromBlobStorage(FILENAME_2);
        verify(paymentOptionRepository, times(1)).findById(PAYMENT_OPTION_ID);
        verify(paymentOptionRepository, times(1)).findById(PAYMENT_OPTION_ID_2);
        verify(ingestionService, times(1)).retryDeadLetterMessage(any(DataCaptureMessage.class));
        verify(blobStorageClient).deleteBlob(FILENAME);
        verify(blobStorageClient, never()).deleteBlob(FILENAME_2);
    }
}
