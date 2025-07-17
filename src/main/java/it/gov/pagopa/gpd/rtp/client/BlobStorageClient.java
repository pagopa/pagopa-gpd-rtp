package it.gov.pagopa.gpd.rtp.client;

import java.util.List;

public interface BlobStorageClient {

    /**
     * Handles saving the file JSON to the blob storage
     *
     * @param stringJSON String JSON to save on blob storage
     * @param fileName   Filename to save the JSON with
     */
    void saveStringJsonToBlobStorage(String stringJSON, String fileName);

    /**
     * Handles retrieving the list of files saved on the blob storage
     *
     * @param year Filter by year
     * @param month Filter by month (requires year)
     * @param day Filter by day (requires month)
     * @param hour Filter by hour (requires day)
     */
    List<String> getBlobList(String year, String month, String day, String hour);

    /**
     * Handles retrieving a file from the blob storage
     *
     * @param fileName Filename to retrieve the JSON with
     */
    byte[] getJsonFromBlobStorage(String fileName);

    /**
     * Handles deleting a file from the blob storage
     *
     * @param fileName Name of the file to delete
     */
    void deleteBlob(String fileName);
}
