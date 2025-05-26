package it.gov.pagopa.gpd.rtp.client;

public interface DeadLetterBlobStorageClient {

    /**
     * Handles saving the ErrorMessage JSON to the blob storage
     *
     * @param errorMessage errorMessage
     * @param fileName Filename to save the JSON with
     */
    void saveErrorMessageToBlobStorage(String errorMessage, String fileName);
}
