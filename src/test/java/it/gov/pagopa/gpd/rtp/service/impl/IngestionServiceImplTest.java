package it.gov.pagopa.gpd.rtp.service.impl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.AnonymizerClient;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.RTPOperationCode;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.exception.FailAndIgnore;
import it.gov.pagopa.gpd.rtp.model.AnonymizerModel;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import it.gov.pagopa.gpd.rtp.repository.TransferRepository;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;

@SpringBootTest(classes = {IngestionServiceImpl.class, ObjectMapper.class})
class IngestionServiceImplTest {
  private static final String REMITTANCE_INFORMATION = "remittanceInformation";
  private static final AnonymizerModel ANONIMIZED_RESPONSE =
      AnonymizerModel.builder().text("anonimizedRemittance").build();
  private static final LocalDateTime DATE_NOW = LocalDateTime.now();

  @MockBean private RTPMessageProducer rtpMessageProducer;
  @MockBean private ProcessingTracker processingTracker;
  @MockBean private FilterService filterService;
  @MockBean private TransferRepository transferRepository;
  @MockBean private PaymentOptionRepository paymentOptionRepository;
  @MockBean private AnonymizerClient anonymizerClient;
  @MockBean private DeadLetterService deadLetterService;
  @MockBean private Acknowledgment acknowledgment;
  @MockBean private RedisCacheRepository redisCacheRepository;
  @SpyBean private ObjectMapper objectMapper;
  @Autowired @InjectMocks private IngestionServiceImpl sut;

  @Captor private ArgumentCaptor<RTPMessage> rtpCaptor;

  @Test
  void ingestPaymentOption_OK_DELETE() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getDeletedPaymentOption();
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);
    when(rtpMessageProducer.sendRTPMessage(any(RTPMessage.class))).thenReturn(true);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer).sendRTPMessage(rtpCaptor.capture());
    verify(filterService, never()).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
    verify(anonymizerClient, never()).anonymize(any());
    RTPMessage captured = rtpCaptor.getValue();

    assertEquals(po.getBefore().getId(), captured.getId());
    assertEquals(po.getTsMs(), captured.getTimestamp());
    assertEquals(RTPOperationCode.DELETE, captured.getOperation());
    assertNull(captured.getIuv());
    assertNull(captured.getSubject());
    assertNull(captured.getDescription());
    assertNull(captured.getEcTaxCode());
    assertNull(captured.getDebtorTaxCode());
    assertNull(captured.getNav());
    assertNull(captured.getDueDate());
    assertEquals(0L, captured.getAmount());
    assertNull(captured.getStatus());
    assertNull(captured.getPspCode());
    assertNull(captured.getPspTaxCode());
  }

  @Test
  void ingestPaymentOption_OK_CREATE() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    Transfer transfer = new Transfer();
    transfer.setRemittanceInformation(REMITTANCE_INFORMATION);
    transfer.setOrganizationFiscalCode(po.getAfter().getOrganizationFiscalCode());
    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of(transfer));

    when(rtpMessageProducer.sendRTPMessage(any(RTPMessage.class))).thenReturn(true);

    when(anonymizerClient.anonymize(any(AnonymizerModel.class))).thenReturn(ANONIMIZED_RESPONSE);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(anonymizerClient).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer).sendRTPMessage(rtpCaptor.capture());
    verify(acknowledgment).acknowledge();
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
    RTPMessage captured = rtpCaptor.getValue();

    assertEquals(po.getAfter().getId(), captured.getId());
    assertEquals(po.getTsMs(), captured.getTimestamp());
    assertEquals(RTPOperationCode.CREATE, captured.getOperation());
    assertEquals(po.getAfter().getIuv(), captured.getIuv());
    assertEquals(ANONIMIZED_RESPONSE.getText(), captured.getSubject());
    assertEquals(po.getAfter().getDescription(), captured.getDescription());
    assertEquals(po.getAfter().getOrganizationFiscalCode(), captured.getEcTaxCode());
    assertEquals(po.getAfter().getFiscalCode(), captured.getDebtorTaxCode());
    assertEquals(po.getAfter().getNav(), captured.getNav());
    assertEquals(po.getAfter().getDueDate(), captured.getDueDate());
    assertEquals(po.getAfter().getAmount(), captured.getAmount());
    assertEquals(po.getAfter().getPaymentPositionStatus(), captured.getStatus());
    assertEquals(po.getAfter().getPspCode(), captured.getPspCode());
    assertEquals(po.getAfter().getPspTaxCode(), captured.getPspTaxCode());
  }

  @Test
  void ingestPaymentOption_OK_UPDATE() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.u);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    Transfer transfer = new Transfer();
    transfer.setRemittanceInformation(REMITTANCE_INFORMATION);
    transfer.setOrganizationFiscalCode(po.getAfter().getOrganizationFiscalCode());
    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of(transfer));

    when(anonymizerClient.anonymize(any(AnonymizerModel.class))).thenReturn(ANONIMIZED_RESPONSE);

    when(rtpMessageProducer.sendRTPMessage(any(RTPMessage.class))).thenReturn(true);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(anonymizerClient).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer).sendRTPMessage(rtpCaptor.capture());
    verify(acknowledgment).acknowledge();
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
    RTPMessage captured = rtpCaptor.getValue();

    assertEquals(po.getAfter().getId(), captured.getId());
    assertEquals(po.getTsMs(), captured.getTimestamp());
    assertEquals(RTPOperationCode.UPDATE, captured.getOperation());
    assertEquals(po.getAfter().getIuv(), captured.getIuv());
    assertEquals(ANONIMIZED_RESPONSE.getText(), captured.getSubject());
    assertEquals(po.getAfter().getDescription(), captured.getDescription());
    assertEquals(po.getAfter().getOrganizationFiscalCode(), captured.getEcTaxCode());
    assertEquals(po.getAfter().getFiscalCode(), captured.getDebtorTaxCode());
    assertEquals(po.getAfter().getNav(), captured.getNav());
    assertEquals(po.getAfter().getDueDate(), captured.getDueDate());
    assertEquals(po.getAfter().getAmount(), captured.getAmount());
    assertEquals(po.getAfter().getPaymentPositionStatus(), captured.getStatus());
    assertEquals(po.getAfter().getPspCode(), captured.getPspCode());
    assertEquals(po.getAfter().getPspTaxCode(), captured.getPspTaxCode());
  }

  @Test
  void ingestPaymentOption_KO_JSON_PROCESSING_EXCEPTION_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    doThrow(JsonProcessingException.class)
        .when(objectMapper)
        .readValue(anyString(), any(TypeReference.class));

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(deadLetterService).sendToDeadLetter(any());
    verify(filterService, never()).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(acknowledgment, never()).nack(any());
  }

  @Test
  void ingestPaymentOption_KO_DEBEZIUM_OPERATION_T_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.t);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_DEBEZIUM_OPERATION_R_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.r);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_DEBEZIUM_OPERATION_M_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.m);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_MESSAGE_NULL_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.m);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_INVALID_PAYMENT_POSITION_STATUS_DISCARDED()
      throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    doThrow(new FailAndIgnore(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP))
        .when(filterService)
        .isValidPaymentOptionForRTPOrElseThrow(any());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_INVALID_TAX_CODE_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    doThrow(new FailAndIgnore(AppError.TAX_CODE_NOT_VALID_FOR_RTP))
        .when(filterService)
        .isValidPaymentOptionForRTPOrElseThrow(any());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_REPLICA_SYNC_FAILED_NO_PO() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.empty());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(acknowledgment).acknowledge();
    verify(acknowledgment, never()).nack(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_REPLICA_SYNC_FAILED_ON_DATE() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW.minus(5L, ChronoUnit.DAYS));
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(acknowledgment).nack(any());
    verify(filterService, never()).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(acknowledgment, never()).acknowledge();
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_INVALID_TRANSFER_CATEGORIES() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of());

    doThrow(new FailAndIgnore(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP))
        .when(filterService)
        .hasValidTransferCategoriesOrElseThrow(any(), any());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(transferRepository).findByPaymentOptionId(anyLong());
    verify(acknowledgment).acknowledge();
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_INVALID_TRANSFER_AMOUNTS() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of());

    doThrow(new FailAndIgnore(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING))
        .when(filterService)
        .hasValidTransferCategoriesOrElseThrow(any(), any());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(transferRepository).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_NO_PRIMARY_TRANSFER() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    Transfer transfer = new Transfer();
    transfer.setOrganizationFiscalCode("differentOrgFiscalCode");
    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of(transfer));

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(transferRepository).findByPaymentOptionId(anyLong());
    verify(acknowledgment).acknowledge();
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_ERROR_SENDING_RTP_MESSAGE() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    Transfer transfer = new Transfer();
    transfer.setRemittanceInformation(REMITTANCE_INFORMATION);
    transfer.setOrganizationFiscalCode(po.getAfter().getOrganizationFiscalCode());
    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of(transfer));

    when(anonymizerClient.anonymize(any(AnonymizerModel.class))).thenReturn(ANONIMIZED_RESPONSE);

    when(rtpMessageProducer.sendRTPMessage(any(RTPMessage.class))).thenReturn(false);

    // test execution
    try {
      sut.ingestPaymentOption(genericMessage);
    } catch (AppException e) {
      assertEquals(AppError.RTP_MESSAGE_NOT_SENT, e.getAppErrorCode());
    }

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(transferRepository).findByPaymentOptionId(anyLong());
    verify(anonymizerClient).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer).sendRTPMessage(any());
    verify(acknowledgment, never()).acknowledge();
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_ERROR_GENERIC() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    Transfer transfer = new Transfer();
    transfer.setRemittanceInformation(REMITTANCE_INFORMATION);
    transfer.setOrganizationFiscalCode(po.getAfter().getOrganizationFiscalCode());
    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of(transfer));

    when(anonymizerClient.anonymize(any(AnonymizerModel.class))).thenReturn(ANONIMIZED_RESPONSE);

    doThrow(RuntimeException.class).when(rtpMessageProducer).sendRTPMessage(any(RTPMessage.class));

    // test execution
    try {
      sut.ingestPaymentOption(genericMessage);
    } catch (RuntimeException e) {
      assertTrue(true);
    }

    verify(filterService).isValidPaymentOptionForRTPOrElseThrow(any());
    verify(filterService).hasValidTransferCategoriesOrElseThrow(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(transferRepository).findByPaymentOptionId(anyLong());
    verify(anonymizerClient).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer).sendRTPMessage(any());
    verify(acknowledgment, never()).acknowledge();
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  private DataCaptureMessage<PaymentOptionEvent> getPaymentOption(
      DebeziumOperationCode debeziumOperationCode) {
    PaymentOptionEvent pp =
        PaymentOptionEvent.builder()
            .id(10L)
            .paymentPositionId(0)
            .amount(0)
            .description("description")
            .dueDate(new Date().getTime())
            .iuv("iuv")
            .lastUpdatedDate(Timestamp.valueOf(DATE_NOW).getTime() * 1000)
            .organizationFiscalCode("organizationFiscalCode")
            .status("PO_PAID")
            .nav("nav")
            .fiscalCode(null)
            .pspCode("pspCode")
            .pspTaxCode("pspTaxCode")
            .paymentPositionStatus(PaymentPositionStatus.VALID)
            .isPartialPayment(false)
            .build();
    return DataCaptureMessage.<PaymentOptionEvent>builder()
        .before(null)
        .after(pp)
        .op(debeziumOperationCode)
        .tsMs(10L)
        .tsNs(0L)
        .tsUs(0L)
        .build();
  }

  private DataCaptureMessage<PaymentOptionEvent> getDeletedPaymentOption() {
    return DataCaptureMessage.<PaymentOptionEvent>builder()
        .before(PaymentOptionEvent.builder().id(0L).build())
        .after(null)
        .op(DebeziumOperationCode.d)
        .tsMs(10L)
        .tsNs(0L)
        .tsUs(0L)
        .build();
  }
}
