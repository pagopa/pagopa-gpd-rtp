package it.gov.pagopa.gpd.rtp.client.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.io.InputStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {BlobStorageClientImpl.class})
class BlobStorageClientImplTest {

    @MockBean
    private BlobServiceClient blobServiceClient;
    @Autowired
    @InjectMocks
    private BlobStorageClientImpl sut;

    @Test
    void saveStringJsonToBlobStorage_OK(){
        BlobContainerClient blobContainerClient = Mockito.mock(BlobContainerClient.class);
        BlobClient blobClient = Mockito.mock(BlobClient.class);

        when(blobContainerClient.getBlobClient(anyString())).thenReturn(blobClient);
        when(blobServiceClient.getBlobContainerClient(anyString())).thenReturn(blobContainerClient);

        sut.saveStringJsonToBlobStorage("stringJSON", "filename");
        verify(blobClient).upload(any(InputStream.class));
    }
}
