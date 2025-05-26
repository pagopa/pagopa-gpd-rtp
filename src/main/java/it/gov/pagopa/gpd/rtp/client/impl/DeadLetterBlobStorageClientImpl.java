package it.gov.pagopa.gpd.rtp.client.impl;

import com.azure.core.http.rest.Response;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlockBlobItem;
import com.azure.storage.blob.options.BlobParallelUploadOptions;
import it.gov.pagopa.gpd.rtp.client.DeadLetterBlobStorageClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.support.ErrorMessage;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

public class DeadLetterBlobStorageClientImpl implements DeadLetterBlobStorageClient {
    @Value("${dead.letter.storage.account.connection.string}")
    private String connectionString;
    @Value("${dead.letter.storage.account.endpoint}")
    private String storageAccount;
    @Value("${dead.letter.storage.container.name}")
    private String containerName;

    private static final String FILE_EXTENSION = ".json";

    private final BlobServiceClient blobServiceClient;

    @Autowired
    private DeadLetterBlobStorageClientImpl(BlobServiceClient blobServiceClient) {
        this.blobServiceClient = Objects.requireNonNullElseGet(blobServiceClient, () -> new BlobServiceClientBuilder()
                .endpoint(storageAccount)
                .connectionString(connectionString)
                .buildClient());
    }

    @Override
    public boolean saveErrorMessageToBlobStorage(ErrorMessage errorMessage, String fileName) {
        InputStream file = new ByteArrayInputStream(errorMessage.toString().getBytes(StandardCharsets.UTF_8));

        //Create the container and return a container client object
        BlobContainerClient blobContainerClient = this.blobServiceClient.getBlobContainerClient(containerName);
        String fileNamePdf = fileName + FILE_EXTENSION;

        //Get a reference to a blob
        BlobClient blobClient = blobContainerClient.getBlobClient(fileNamePdf);

        //Upload the blob
        Response<BlockBlobItem> blockBlobItemResponse = blobClient.uploadWithResponse(
                new BlobParallelUploadOptions(
                        file
                ), null, null);

        return blockBlobItemResponse.getStatusCode() == HttpStatus.CREATED.value();
    }
}
