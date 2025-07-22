package it.gov.pagopa.gpd.rtp.client.impl;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.models.BlobItem;
import com.azure.storage.blob.models.BlobStorageException;
import com.azure.storage.blob.models.ListBlobsOptions;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static it.gov.pagopa.gpd.rtp.util.CommonUtility.sanitizeLogInput;

@Service
@Slf4j
public class BlobStorageClientImpl implements BlobStorageClient {
    private static final String FILE_EXTENSION = ".json";
    private final BlobContainerClient blobContainerClient;

    @Autowired
    BlobStorageClientImpl(BlobContainerClient blobContainerClient) {
        this.blobContainerClient = blobContainerClient;
    }

    @Override
    public void saveStringJsonToBlobStorage(String stringJSON, String fileName) {
        InputStream file = new ByteArrayInputStream(stringJSON.getBytes(StandardCharsets.UTF_8));
        String fileNamePdf = fileName + FILE_EXTENSION;

        BlobClient blobClient = blobContainerClient.getBlobClient(fileNamePdf);
        blobClient.upload(file);
    }

    @Override
    public List<String> getBlobList(String year, String month, String day, String hour) {
        String blobPrefix = null;
        if (year != null) {
            blobPrefix = year;
            if (month != null) {
                blobPrefix = blobPrefix.concat("/" + month);
                if (day != null) {
                    blobPrefix = blobPrefix.concat("/" + day);
                    if (hour != null) {
                        blobPrefix = blobPrefix.concat("/" + hour);
                    }
                }
            }
        }

        ListBlobsOptions options = new ListBlobsOptions().setPrefix(blobPrefix);
        return blobContainerClient.listBlobs(options, Duration.ofSeconds(30))
                .stream().map(BlobItem::getName).toList();
    }

    @Override
    public byte[] getJSONFromBlobStorage(String fileName) {
        BlobClient blobClient = blobContainerClient.getBlobClient(fileName);
        try {
            return blobClient.downloadContent().toBytes();
        } catch (UncheckedIOException e) {
            log.error("I/O error downloading the JSON from Blob Storage");
            throw new AppException(AppError.BLOB_STORAGE_IO_EXCEPTION);
        } catch (BlobStorageException e) {
            String sanitizedLog = sanitizeLogInput(fileName);
            if (e.getStatusCode() == 404) {
                log.error("JSON with name: {} not found in Blob Storage: {}", sanitizedLog, blobClient.getAccountName());
                throw new AppException(AppError.BLOB_STORAGE_ATTACHMENT_NOT_FOUND);
            }
            log.error("Unable to download the JSON with name: {} from Blob Storage: {}. Error message from server: {}",
                    sanitizedLog,
                    blobClient.getAccountName(),
                    e.getServiceMessage());
            throw new AppException(AppError.BLOB_STORAGE_HTTP_ERROR);
        }
    }

    @Override
    public boolean deleteBlob(String fileName) {
        BlobClient blobClient = blobContainerClient.getBlobClient(fileName);

        return blobClient.deleteIfExists();
    }
}
