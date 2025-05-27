package it.gov.pagopa.gpd.rtp.config;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BlobStorageClientConfig {

    @Bean
    public BlobServiceClient blobServiceClientBean(
            @Value("${dead.letter.storage.account.connection.string}") String connectionString,
            @Value("${dead.letter.storage.account.endpoint}") String storageAccount
    ) {
        return new BlobServiceClientBuilder()
                .endpoint(storageAccount)
                .connectionString(connectionString)
                .buildClient();
    }
}
