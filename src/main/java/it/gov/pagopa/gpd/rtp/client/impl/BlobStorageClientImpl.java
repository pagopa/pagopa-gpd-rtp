package it.gov.pagopa.gpd.rtp.client.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class BlobStorageClientImpl implements BlobStorageClient {
    private final String containerName;
    private static final String FILE_EXTENSION = ".json";
    private final BlobServiceClient blobServiceClient;

    @Autowired
    BlobStorageClientImpl(
            BlobServiceClient blobServiceClient,
            @Value("${dead.letter.storage.container.name}") String containerName) {
        this.blobServiceClient = blobServiceClient;
        this.containerName = containerName;
    }

    @Override
    public void saveStringJsonToBlobStorage(String stringJSON, String fileName) {
        InputStream file = new ByteArrayInputStream(stringJSON.getBytes(StandardCharsets.UTF_8));

        //Create the container and return a container client object
        BlobContainerClient blobContainerClient = this.blobServiceClient.getBlobContainerClient(containerName);
        String fileNamePdf = fileName + FILE_EXTENSION;

        //Get a reference to a blob
        BlobClient blobClient = blobContainerClient.getBlobClient(fileNamePdf);

        //Upload the blob
        blobClient.upload(file);
    }
}
