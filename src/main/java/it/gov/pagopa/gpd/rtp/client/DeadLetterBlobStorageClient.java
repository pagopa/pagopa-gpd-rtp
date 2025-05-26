package it.gov.pagopa.gpd.rtp.client;

import org.springframework.messaging.support.ErrorMessage;

public interface DeadLetterBlobStorageClient {

    /**
     * Handles saving the ErrorMessage JSON to the blob storage
     *
     * @param errorMessage errorMessage
     * @param paymentOptionId Filename to save the JSON with
     * @return boolean
     */
    boolean saveErrorMessageToBlobStorage(ErrorMessage errorMessage, String paymentOptionId);
}
