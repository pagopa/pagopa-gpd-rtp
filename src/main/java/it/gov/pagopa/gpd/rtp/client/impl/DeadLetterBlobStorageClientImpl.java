package it.gov.pagopa.gpd.rtp.client.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import it.gov.pagopa.gpd.rtp.client.DeadLetterBlobStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Service
public class DeadLetterBlobStorageClientImpl implements DeadLetterBlobStorageClient {

    @Value("${dead.letter.storage.container.name}")
    private String containerName;

    private static final String FILE_EXTENSION = ".json";

    private final BlobServiceClient blobServiceClient;

    @Autowired
    DeadLetterBlobStorageClientImpl(
            @Value("${dead.letter.storage.account.connection.string}") String connectionString,
            @Value("${dead.letter.storage.account.endpoint}") String storageAccount
    ) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .endpoint(storageAccount)
                .connectionString(connectionString)
                .buildClient();
    }

    @Override
    public void saveErrorMessageToBlobStorage(String errorMessage, String fileName) {
        InputStream file = new ByteArrayInputStream(errorMessage.getBytes(StandardCharsets.UTF_8));

        //Create the container and return a container client object
        BlobContainerClient blobContainerClient = this.blobServiceClient.getBlobContainerClient(containerName);
        String fileNamePdf = fileName + FILE_EXTENSION;

        //Get a reference to a blob
        BlobClient blobClient = blobContainerClient.getBlobClient(fileNamePdf);

        //Upload the blob
        blobClient.upload(file);
    }
}
