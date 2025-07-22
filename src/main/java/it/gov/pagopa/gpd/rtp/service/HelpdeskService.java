package it.gov.pagopa.gpd.rtp.service;

import com.fasterxml.jackson.core.JsonProcessingException;

import java.util.List;

public interface HelpdeskService {

    /**
     * Retrieve list of filenames of the blobs in the blob storage
     *
     * @param year  Filter by year
     * @param month Filter by month (requires year)
     * @param day   Filter by day (requires month)
     * @param hour  Filter by hour (requires day)
     */
    List<String> getBlobList(String year, String month, String day, String hour);

    /**
     * Retrieve failed PaymentOption Messages from dead letter storage
     *
     * @param fileName Blob filename
     */
    String getJSONFromBlobStorage(String fileName);

    /**
     * Retry failed PaymentOption Messages by JSON blob filename
     *
     * @param fileName Blob filename
     */
    String retryMessage(String fileName) throws JsonProcessingException;
}
