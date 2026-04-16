package it.gov.pagopa.gpd.rtp.service;

import it.gov.pagopa.gpd.rtp.model.helpdesk.RetryDeadLetterResponse;

import java.util.List;

public interface HelpdeskService {

  /**
   * Retrieve list of filenames of the blobs in the blob storage
   *
   * @param year Filter by year
   * @param month Filter by month (requires year)
   * @param day Filter by day (requires month)
   * @param hour Filter by hour (requires day)
   * @param maxMessages Retrieve only the defined amount of messages
   */
  List<String> getBlobList(String year, String month, String day, String hour, int maxMessages);

  /**
   * Retrieve failed PaymentOption Messages from dead letter storage
   *
   * @param fileName Blob filename
   */
  String getJSONFromBlobStorage(String fileName);

  /**
   * Retry failed PaymentOption Messages by JSON blob filename
   *
   * @param fileNames A list of Blob filenames
   * @param minutesOffset Integer to ignore messages newer than the defined minutes
   */
  RetryDeadLetterResponse retryMessages(List<String> fileNames, int minutesOffset);
}
