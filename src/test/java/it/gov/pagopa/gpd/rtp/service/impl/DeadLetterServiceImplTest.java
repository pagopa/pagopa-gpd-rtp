package it.gov.pagopa.gpd.rtp.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.verify;

import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.messaging.support.GenericMessage;

@SpringBootTest(classes = DeadLetterServiceImpl.class)
class DeadLetterServiceImplTest {

  private static final String CDC_MESSAGE_ID = "12345";
  private static final byte[] CDC_MESSAGE_KEY = ("{\"id\":\"" + CDC_MESSAGE_ID + "\"}").getBytes();
  private static final String ORIGINAL_MESSAGE_PAYLOAD =
      "\"originalMessage\":\"[ERROR] Retrieving original message payload\"";
  private static final String DEAD_LETTER_ID_FIELD = "\"id\":";
  private static final String DEAD_LETTER_CAUSE_FIELD = "\"cause\":\"";
  private static final String DEAD_LETTER_ERROR_CODE_FIELD = "\"errorCode\":\"";

  @MockBean private BlobStorageClient blobStorageClient;

  @MockBean private ProcessingTracker processingTracker;

  @Captor private ArgumentCaptor<String> errorMessageCaptor;

  @Captor private ArgumentCaptor<String> filePathCaptor;

  @Autowired private DeadLetterServiceImpl sut;

  @Test
  void sendToDeadLetter_OK() {
    ErrorMessage errorMessage = buildErrorMessage();

    assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

    verify(blobStorageClient)
        .saveStringJsonToBlobStorage(errorMessageCaptor.capture(), filePathCaptor.capture());
    String capturedErrorMessage = errorMessageCaptor.getValue();

    assertTrue(capturedErrorMessage.contains(DEAD_LETTER_ID_FIELD + CDC_MESSAGE_ID));
    assertTrue(
        capturedErrorMessage.contains(
            DEAD_LETTER_CAUSE_FIELD + AppError.INTERNAL_SERVER_ERROR.getDetails()));
    assertTrue(
        capturedErrorMessage.contains(
            DEAD_LETTER_ERROR_CODE_FIELD + AppError.INTERNAL_SERVER_ERROR.name()));

    String capturedFilePath = filePathCaptor.getValue();

    assertNotNull(capturedFilePath);
  }

  @Test
  void sendToDeadLetter_KO_ERROR_PARSING_MESSAGE_KEY() {
    ErrorMessage errorMessage = buildErrorMessageWithoutOriginalMessageHeaders();

    assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

    verify(blobStorageClient).saveStringJsonToBlobStorage(errorMessageCaptor.capture(), any());
    String capturedErrorMessage = errorMessageCaptor.getValue();

    assertTrue(
        capturedErrorMessage.contains(DEAD_LETTER_ID_FIELD + errorMessage.getHeaders().getId()));
  }

  @Test
  void sendToDeadLetter__KO_NULL_ORIGINAL_MESSAGE() {
    ErrorMessage errorMessage = buildErrorMessageWithoutMessage();

    assertDoesNotThrow(() -> sut.sendToDeadLetter(errorMessage));

    verify(blobStorageClient).saveStringJsonToBlobStorage(errorMessageCaptor.capture(), any());
    String capturedErrorMessage = errorMessageCaptor.getValue();

    assertTrue(
        capturedErrorMessage.contains(DEAD_LETTER_ID_FIELD + errorMessage.getHeaders().getId()));
    assertTrue(capturedErrorMessage.contains(ORIGINAL_MESSAGE_PAYLOAD));
  }

  private ErrorMessage buildErrorMessage() {
    AppException appException = new AppException(AppError.INTERNAL_SERVER_ERROR);

    MessageHeaders originalMessageHeaders =
        new MessageHeaders(Map.of(KafkaHeaders.RECEIVED_KEY, CDC_MESSAGE_KEY));
    MessageHeaders errorMessageHeaders = new MessageHeaders(Collections.emptyMap());
    Message<byte[]> originalMessage =
        new GenericMessage<>(
            String.valueOf(new DataCaptureMessage<>()).getBytes(), originalMessageHeaders);

    return new ErrorMessage(
        new MessageHandlingException(originalMessage, appException),
        errorMessageHeaders,
        originalMessage);
  }

  private ErrorMessage buildErrorMessageWithoutMessage() {
    AppException appException = new AppException(AppError.INTERNAL_SERVER_ERROR);

    return new ErrorMessage(new Exception(appException), Collections.emptyMap());
  }

  private ErrorMessage buildErrorMessageWithoutOriginalMessageHeaders() {
    AppException appException = new AppException(AppError.INTERNAL_SERVER_ERROR);

    MessageHeaders originalMessageHeaders = new MessageHeaders(Collections.emptyMap());
    MessageHeaders errorMessageHeaders = new MessageHeaders(Collections.emptyMap());
    Message<byte[]> originalMessage =
        new GenericMessage<>(
            String.valueOf(new DataCaptureMessage<>()).getBytes(), originalMessageHeaders);

    return new ErrorMessage(new Exception(appException), errorMessageHeaders, originalMessage);
  }
}
