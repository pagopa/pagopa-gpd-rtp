package it.gov.pagopa.gpd.rtp.service.impl;

import static it.gov.pagopa.gpd.rtp.util.Constants.CUSTOM_EVENT;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import it.gov.pagopa.gpd.rtp.client.AnonymizerClient;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.RTPOperationCode;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.FailAndIgnore;
import it.gov.pagopa.gpd.rtp.exception.FailAndNotify;
import it.gov.pagopa.gpd.rtp.exception.FailAndPostpone;
import it.gov.pagopa.gpd.rtp.model.AnonymizerModel;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import it.gov.pagopa.gpd.rtp.repository.TransferRepository;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class IngestionServiceImpl implements IngestionService {

  private final ObjectMapper objectMapper;
  private final RTPMessageProducer rtpMessageProducer;
  private final FilterService filterService;
  private final TransferRepository transferRepository;
  private final PaymentOptionRepository paymentOptionRepository;
  private final AnonymizerClient anonymizerClient;
  private final DeadLetterService deadLetterService;
  private final ProcessingTracker processingTracker;
  private final RedisCacheRepository redisCacheRepository;
  private final TelemetryClient telemetryClient;

  @Value("${max.retry.db.replica}")
  private Integer maxRetryDbReplica;

  public void ingestPaymentOption(Message<String> message) {
    try {
      processingTracker.messageProcessingStarted();
      handleMessage(message);
    } finally {
      processingTracker.messageProcessingFinished();
    }
  }

  private void handleMessage(Message<?> message) {
    Acknowledgment acknowledgment = null;
    DataCaptureMessage<PaymentOptionEvent> paymentOption = null;
    try {
      acknowledgment = getAck(message);

      paymentOption = parseMessage(message);
      RTPMessage rtpMessage = createRTPMessageOrElseThrow(paymentOption);

      boolean response = this.rtpMessageProducer.sendRTPMessage(rtpMessage);
      checkResponse(response);

      log.debug("RTP Message sent to eventhub at {}", LocalDateTime.now());
      acknowledgment.acknowledge();

    } catch (FailAndPostpone e) {
      assert paymentOption != null : "paymentOption cannot be null";
      handleRetry(message, getOptionEvent(paymentOption), e, acknowledgment);
    } catch (FailAndIgnore e) {
      log.info("Message ignored {}", e.getMessage());
      assert acknowledgment != null : "acknowledgment cannot be null";
      acknowledgment.acknowledge();
    } catch (FailAndNotify e) {
      log.error("Unexpected error raised", e);
      sendCustomEvent(e);
      throw e;
    } catch (Exception e) {
      log.error("Unexpected error raised", e);
      FailAndNotify failAndNotify = new FailAndNotify(AppError.INTERNAL_SERVER_ERROR, e);
      sendCustomEvent(failAndNotify);
      throw failAndNotify;
    }
  }

  private void sendCustomEvent(FailAndNotify e) {
    Map<String, String> props =
        Map.of(
            "type", e.getAppErrorCode().name(),
            "title", e.getAppErrorCode().getTitle(),
            "details", e.getAppErrorCode().getDetails(),
            "cause", e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
    telemetryClient.trackEvent(CUSTOM_EVENT, props, null);
  }

  private static String getOptionEvent(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
    return Optional.ofNullable(paymentOption.getBefore())
        .orElse(paymentOption.getAfter())
        .getId()
        .toString();
  }

  private DataCaptureMessage<PaymentOptionEvent> parseMessage(Message<?> message) {
    // Discard null messages
    if (message.getHeaders().getId() == null
        || message.getPayload() == null
        || !(message.getPayload() instanceof String msg)) {
      log.debug("NULL message ignored at {}", LocalDateTime.now());
      throw new FailAndIgnore(AppError.NULL_MESSAGE);
    }

    log.debug(
        "PaymentOption ingestion called at {} for payment options with message id {}",
        LocalDateTime.now(),
        message.getHeaders().getId());

    return parseMessage(message, msg);
  }

  @NotNull
  private static Acknowledgment getAck(Message<?> message) {
    Acknowledgment acknowledgment =
        message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
    if (acknowledgment == null) {
      throw new FailAndNotify(AppError.ACKNOWLEDGMENT_NOT_PRESENT);
    }
    return acknowledgment;
  }

  /**
   * Handles retry logic for failed messages.
   *
   * <p>If the retry count for the message is less than 3, the message is postponed and the retry
   * count is increased by 1. If the retry count is 3 or more, the message is saved to the dead
   * letter storage account.
   *
   * @param message The message to be retried.
   * @param e The exception that caused the retry.
   * @param acknowledgment The acknowledgment object to be used to nack the message.
   */
  private void handleRetry(
      Message<?> message,
      String paymentOptionId,
      FailAndPostpone e,
      Acknowledgment acknowledgment) {

    // get retry count
    int retryCount = redisCacheRepository.getRetryCount(paymentOptionId);
    if (retryCount < maxRetryDbReplica) {
      // if retry count < n then postpone the message and add 1 to the retry count
      log.warn("Retry reading message after", e);
      redisCacheRepository.setRetryCount(paymentOptionId, retryCount + 1);
      acknowledgment.nack(Duration.ofSeconds(1));
    } else {
      // if retry count >= n then save the message to dead letter
      this.deadLetterService.sendToDeadLetter(
          new ErrorMessage(new MessageHandlingException(message, e), message));
      redisCacheRepository.deleteRetryCount(paymentOptionId);
    }
  }

  private static void checkResponse(boolean response) {
    if (!response) {
      throw new FailAndNotify(AppError.RTP_MESSAGE_NOT_SENT);
    }
  }

  private DataCaptureMessage<PaymentOptionEvent> parseMessage(Message<?> message, String msg) {
    try {
      return this.objectMapper.readValue(msg, new TypeReference<>() {});
    } catch (JsonProcessingException e) {
      this.deadLetterService.sendToDeadLetter(
          new ErrorMessage(
              new MessageHandlingException(
                  message, new FailAndIgnore(AppError.JSON_NOT_PROCESSABLE)),
              message));
      throw new FailAndIgnore(AppError.JSON_NOT_PROCESSABLE);
    }
  }

  private RTPMessage createRTPMessageOrElseThrow(
      DataCaptureMessage<PaymentOptionEvent> paymentOption) {
    if (paymentOption.getOp().equals(DebeziumOperationCode.d)) {
      // Map RTP delete message
      return mapRTPDeleteMessage(paymentOption);
    }
    if (paymentOption.getOp().equals(DebeziumOperationCode.c)
        || paymentOption.getOp().equals(DebeziumOperationCode.u)) {
      // Filter paymentOption message, throws AppException
      this.filterService.isValidPaymentOptionForRTPOrElseThrow(paymentOption);

      PaymentOptionEvent valuesAfter = paymentOption.getAfter();

      log.debug(
          "PaymentOption ingestion called at {} with payment option id {}",
          LocalDateTime.now(),
          valuesAfter.getId());

      verifyDBReplicaSync(valuesAfter);

      // Retrieve Transfer's data
      List<Transfer> transferList =
          this.transferRepository.findByPaymentOptionId(valuesAfter.getId());
      // Filter based on Transfer's categories, throws AppException
      this.filterService.hasValidTransferCategoriesOrElseThrow(valuesAfter, transferList);
      String remittanceInformation = getRemittanceInformation(valuesAfter, transferList);

      return mapRTPMessage(paymentOption, remittanceInformation);
    }
    throw new FailAndIgnore(AppError.CDC_OPERATION_NOT_VALID_FOR_RTP);
  }

  private void verifyDBReplicaSync(PaymentOptionEvent valuesAfter) {
    PaymentOption poFromDBReplica =
        paymentOptionRepository
            .findById(valuesAfter.getId())
            .orElseThrow(() -> new FailAndPostpone(AppError.PAYMENT_OPTION_NOT_FOUND));
    Instant poMessageInstant = Instant.ofEpochMilli(valuesAfter.getLastUpdatedDate() / 1000);
    LocalDateTime poMessageDate = LocalDateTime.ofInstant(poMessageInstant, ZoneOffset.UTC);
    if (poFromDBReplica == null) {
      throw new FailAndPostpone(AppError.PAYMENT_OPTION_NOT_FOUND);
    }
    if (poFromDBReplica.getLastUpdatedDate().isBefore(poMessageDate)) {
      throw new FailAndPostpone(AppError.DB_REPLICA_NOT_UPDATED);
    }
  }

  private String getRemittanceInformation(
      PaymentOptionEvent valuesAfter, List<Transfer> transferList) {
    Transfer primaryTransfer =
        transferList.stream()
            .filter(
                el ->
                    el.getOrganizationFiscalCode().equals(valuesAfter.getOrganizationFiscalCode()))
            .findFirst()
            .orElseThrow(() -> new FailAndIgnore(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP));
    return primaryTransfer.getRemittanceInformation();
  }

  private String anonymizePII(String text){
    AnonymizerModel request =
            AnonymizerModel.builder().text(text).build();
    return this.anonymizerClient.anonymize(request).getText();
  }

  private RTPMessage mapRTPMessage(
      DataCaptureMessage<PaymentOptionEvent> paymentOption, String remittanceInformation) {
    PaymentOptionEvent valuesAfter = paymentOption.getAfter();
    return RTPMessage.builder()
        .id(valuesAfter.getId())
        .operation(
            paymentOption.getOp().equals(DebeziumOperationCode.c)
                ? RTPOperationCode.CREATE
                : RTPOperationCode.UPDATE)
        .timestamp(paymentOption.getTsMs())
        .iuv(valuesAfter.getIuv())
        .subject(anonymizePII(remittanceInformation))
        .description(anonymizePII(valuesAfter.getDescription()))
        .ecTaxCode(valuesAfter.getOrganizationFiscalCode())
        .debtorTaxCode(valuesAfter.getFiscalCode())
        .nav(valuesAfter.getNav())
        .dueDate(valuesAfter.getDueDate())
        .amount(valuesAfter.getAmount())
        .status(valuesAfter.getPaymentPositionStatus())
        .pspCode(valuesAfter.getPspCode())
        .pspTaxCode(valuesAfter.getPspTaxCode())
        .isPartialPayment(valuesAfter.getIsPartialPayment())
        .build();
  }

  private RTPMessage mapRTPDeleteMessage(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
    return RTPMessage.builder()
        .id(paymentOption.getBefore().getId())
        .operation(RTPOperationCode.DELETE)
        .timestamp(paymentOption.getTsMs())
        .build();
  }
}
