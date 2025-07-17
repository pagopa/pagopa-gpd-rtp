package it.gov.pagopa.gpd.rtp.client.impl;

import com.azure.core.http.rest.PagedIterable;
import com.azure.core.util.BinaryData;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.DeadLetterMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@SpringBootTest(classes = {BlobStorageClientImpl.class})
class BlobStorageClientImplTest {
    public static final String YEAR = "2025";
    public static final String MONTH = "6";
    public static final String DAY = "17";
    public static final String HOUR = "12";
    public static final String FILENAME = "testFilename.json";
    @MockBean
    private BlobServiceClient blobServiceClient;
    @SpyBean
    private ObjectMapper objectMapper;
    @Autowired
    @InjectMocks
    private BlobStorageClientImpl sut;
    @Captor
    ArgumentCaptor<ListBlobsOptions> listBlobsOptionsArgumentCaptor;

    @Test
    void saveStringJsonToBlobStorage_OK() {
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);

        sut.saveStringJsonToBlobStorage("stringJSON", "filename");
        verify(blobClient).upload(any(InputStream.class));
    }

    @Test
    void getBlobList_OK_no_date_filter() {
        BlobItem blob = new BlobItem();
        blob.setName(FILENAME);
        PagedIterable pagedIterable = mock(PagedIterable.class);

        when(pagedIterable.stream()).thenReturn(Stream.of(blob));

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(pagedIterable);

        List<String> response = sut.getBlobList(null, null, null, null);
        assertTrue(response.contains(FILENAME));
        verify(blobContainerClient).listBlobs(listBlobsOptionsArgumentCaptor.capture(), any(Duration.class));

        assertNull(listBlobsOptionsArgumentCaptor.getValue().getPrefix());
    }

    @Test
    void getBlobList_OK_all_date_filter() {
        BlobItem blob = new BlobItem();
        blob.setName(FILENAME);
        PagedIterable pagedIterable = mock(PagedIterable.class);

        when(pagedIterable.stream()).thenReturn(Stream.of(blob));

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(pagedIterable);

        List<String> response = sut.getBlobList(YEAR, MONTH, DAY, HOUR);
        assertTrue(response.contains(FILENAME));
        verify(blobContainerClient).listBlobs(listBlobsOptionsArgumentCaptor.capture(), any(Duration.class));

        assertEquals(String.format("%s/%s/%s/%s", YEAR, MONTH, DAY, HOUR), listBlobsOptionsArgumentCaptor.getValue().getPrefix());
    }

    @Test
    void getBlobList_OK_only_hour_filter() {
        BlobItem blob = new BlobItem();
        blob.setName(FILENAME);
        PagedIterable pagedIterable = mock(PagedIterable.class);

        when(pagedIterable.stream()).thenReturn(Stream.of(blob));

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        when(blobContainerClient.listBlobs(any(ListBlobsOptions.class), any(Duration.class))).thenReturn(pagedIterable);

        List<String> response = sut.getBlobList(null, null, null, HOUR);
        assertTrue(response.contains(FILENAME));
        verify(blobContainerClient).listBlobs(listBlobsOptionsArgumentCaptor.capture(), any(Duration.class));

        assertNull(listBlobsOptionsArgumentCaptor.getValue().getPrefix());
    }

    @Test
    void getJSONFromBlobStorage_OK() throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage = DeadLetterMessage.builder().id("id").originalMessage(DataCaptureMessage.<PaymentOptionEvent>builder().build()).build();
        String json = objectMapper.writeValueAsString(deadLetterMessage);

        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        when(blobClient.downloadContent()).thenReturn(BinaryData.fromString(json));

        byte[] response = assertDoesNotThrow(() -> sut.getJSONFromBlobStorage(FILENAME));
        assertEquals(json, new String(response));
        verify(blobClient).downloadContent();
    }

    @Test
    void getJSONFromBlobStorage_KO_IO_error() {
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        when(blobClient.downloadContent()).thenThrow(new UncheckedIOException(new IOException()));

        assertThrows(AppException.class, () -> sut.getJSONFromBlobStorage(FILENAME));
    }

    @Test
    void getJSONFromBlobStorage_KO_blob_storage_exception_404() {
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        BlobStorageException blobStorageException = mock(BlobStorageException.class);
        when(blobStorageException.getStatusCode()).thenReturn(HttpStatus.NOT_FOUND.value());
        when(blobClient.downloadContent()).thenThrow(blobStorageException);

        assertThrows(AppException.class, () -> sut.getJSONFromBlobStorage(FILENAME));
    }

    @Test
    void getJSONFromBlobStorage_KO_blob_storage_exception_generic() {
        BlobContainerClient blobContainerClient = mock(BlobContainerClient.class);
        BlobClient blobClient = mock(BlobClient.class);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);
        BlobStorageException blobStorageException = mock(BlobStorageException.class);
        when(blobStorageException.getStatusCode()).thenReturn(HttpStatus.INTERNAL_SERVER_ERROR.value());
        when(blobClient.downloadContent()).thenThrow(blobStorageException);

        assertThrows(AppException.class, () -> sut.getJSONFromBlobStorage(FILENAME));
    }
}
