package it.gov.pagopa.gpd.rtp.config;

import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStorageClientConfig {

    @Bean
    public BlobContainerClient blobContainerClientBean(
            @Value("${dead.letter.storage.account.connection.string}") String connectionString,
            @Value("${dead.letter.storage.account.endpoint}") String storageAccount,
            @Value("${dead.letter.storage.container.name}") String containerName
    ) {
        return new BlobServiceClientBuilder()
                .endpoint(storageAccount)
                .connectionString(connectionString)
                .buildClient()
                .getBlobContainerClient(containerName);
    }
}
