package it.gov.pagopa.gpd.rtp.service.impl;

import static it.gov.pagopa.gpd.rtp.util.Constants.LOG_PREFIX;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import it.gov.pagopa.gpd.rtp.repository.TransferRepository;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

  public void ingestPaymentOption(Message<String> message) {
    try {
      processingTracker.messageProcessingStarted();
      handleMessage(message);
    } finally {
      processingTracker.messageProcessingFinished();
    }
  }

  private void handleMessage(Message<String> message) {
    Acknowledgment acknowledgment =
        message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);
    if (acknowledgment == null) {
      throw new FailAndNotify(AppError.ACKNOWLEDGMENT_NOT_PRESENT);
    }

    try {
      // Discard null messages
      if (message.getHeaders().getId() == null) {
        log.debug("{} NULL message ignored at {}", LOG_PREFIX, LocalDateTime.now());
        throw new FailAndIgnore(AppError.NULL_MESSAGE);
      }

      log.debug(
          "PaymentOption ingestion called at {} for payment options with message id {}",
          LocalDateTime.now(),
          message.getHeaders().getId());
      String msg = message.getPayload();

      DataCaptureMessage<PaymentOptionEvent> paymentOption = parseMessage(message, msg);
      RTPMessage rtpMessage = createRTPMessageOrElseThrow(paymentOption);

      boolean response = this.rtpMessageProducer.sendRTPMessage(rtpMessage);
      checkResponse(response);

      log.debug("{} RTPMessage sent to eventhub at {}", LOG_PREFIX, LocalDateTime.now());
      acknowledgment.acknowledge();
    } catch (FailAndPostpone e) {
      log.error(LOG_PREFIX + " Retry reading message after", e);
      acknowledgment.nack(Duration.ofSeconds(1));
    } catch (FailAndIgnore e) {
      log.info("{} Message ignored", LOG_PREFIX);
      acknowledgment.acknowledge();
    } catch (FailAndNotify e) {
      log.error(LOG_PREFIX + " Unexpected error raised", e);
      throw e;
    } catch (Exception e) {
      log.error(LOG_PREFIX + " Unexpected error raised", e);
      throw new FailAndNotify(AppError.INTERNAL_SERVER_ERROR, e);
    }
  }

  private static void checkResponse(boolean response) {
    if (!response) {
      throw new FailAndNotify(AppError.RTP_MESSAGE_NOT_SENT);
    }
  }

  private DataCaptureMessage<PaymentOptionEvent> parseMessage(Message<String> message, String msg) {
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
      String remittanceInformation = anonymizeRemittanceInformation(valuesAfter, transferList);

      return mapRTPMessage(paymentOption, remittanceInformation);
    }
    throw new FailAndIgnore(AppError.CDC_OPERATION_NOT_VALID_FOR_RTP);
  }

  private void verifyDBReplicaSync(PaymentOptionEvent valuesAfter) {
    PaymentOption poFromDBReplica =
        paymentOptionRepository
            .findById(valuesAfter.getId())
            .orElseThrow(() -> new FailAndIgnore(AppError.PAYMENT_OPTION_NOT_FOUND));
    Instant poMessageInstant = Instant.ofEpochMilli(valuesAfter.getLastUpdatedDate() / 1000);
    LocalDateTime poMessageDate = LocalDateTime.ofInstant(poMessageInstant, ZoneOffset.UTC);
    if (poFromDBReplica == null) {
      throw new FailAndIgnore(AppError.PAYMENT_OPTION_NOT_FOUND);
    }
    if (poFromDBReplica.getLastUpdatedDate().isBefore(poMessageDate)) {
      throw new FailAndPostpone(AppError.DB_REPLICA_NOT_UPDATED);
    }
  }

  private String anonymizeRemittanceInformation(
      PaymentOptionEvent valuesAfter, List<Transfer> transferList) {
    Transfer primaryTransfer =
        transferList.stream()
            .filter(
                el ->
                    el.getOrganizationFiscalCode().equals(valuesAfter.getOrganizationFiscalCode()))
            .findFirst()
            .orElseThrow(() -> new FailAndIgnore(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP));
    AnonymizerModel request =
        AnonymizerModel.builder().text(primaryTransfer.getRemittanceInformation()).build();
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
        .subject(remittanceInformation)
        .description(valuesAfter.getDescription())
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
