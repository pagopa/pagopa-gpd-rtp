package it.gov.pagopa.gpd.rtp.client;

public interface BlobStorageClient {

    /**
     * Handles saving the file JSON to the blob storage
     *
     * @param stringJSON String JSON to save on blob storage
     * @param fileName Filename to save the JSON with
     */
    void saveStringJsonToBlobStorage(String stringJSON, String fileName);
}
