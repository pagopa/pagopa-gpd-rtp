package it.gov.pagopa.gpd.rtp.service.impl;

import static it.gov.pagopa.gpd.rtp.utils.EntityUtils.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.applicationinsights.TelemetryClient;
import it.gov.pagopa.gpd.rtp.client.AnonymizerClient;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.entity.PaymentPosition;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.entity.enumeration.ServiceType;
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
import it.gov.pagopa.gpd.rtp.repository.DebtPositionRepository;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import it.gov.pagopa.gpd.rtp.repository.TransferRepository;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import java.time.temporal.ChronoUnit;
import java.util.*;

import it.gov.pagopa.gpd.rtp.utils.EntityUtils;
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
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest(
    classes = {IngestionServiceImpl.class, ObjectMapper.class},
    properties = "max.retry.db.replica=3")
class IngestionServiceImplTest {
  private static final String REMITTANCE_INFORMATION = "remittanceInformation";
  private static final AnonymizerModel ANONIMIZED_RESPONSE =
      AnonymizerModel.builder().text("anonimizedRemittance").build();


  @MockBean private RTPMessageProducer rtpMessageProducer;
  @MockBean private ProcessingTracker processingTracker;
  @MockBean private FilterService filterService;
  @MockBean private TransferRepository transferRepository;
  @MockBean private PaymentOptionRepository paymentOptionRepository;
  @MockBean private DebtPositionRepository debtPositionRepository;
  @MockBean private AnonymizerClient anonymizerClient;
  @MockBean private DeadLetterService deadLetterService;
  @MockBean private Acknowledgment acknowledgment;
  @MockBean private RedisCacheRepository redisCacheRepository;
  @MockBean private TelemetryClient telemetryClient;
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
    verify(filterService, never()).filterByTaxCode(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
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
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setIupd("123456");
    debtPosition.setStatus(PaymentPositionStatus.VALID);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    Transfer transfer = new Transfer();
    transfer.setRemittanceInformation(REMITTANCE_INFORMATION);
    transfer.setOrganizationFiscalCode(po.getAfter().getOrganizationFiscalCode());
    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of(transfer));

    when(rtpMessageProducer.sendRTPMessage(any(RTPMessage.class))).thenReturn(true);

    when(anonymizerClient.anonymize(any(AnonymizerModel.class))).thenReturn(ANONIMIZED_RESPONSE);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).filterByTaxonomy(any(), any());
    verify(anonymizerClient, times(2)).anonymize(any(AnonymizerModel.class));
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
    assertEquals(ANONIMIZED_RESPONSE.getText(), captured.getDescription());
    assertEquals(po.getAfter().getOrganizationFiscalCode(), captured.getEcTaxCode());
    assertEquals(po.getAfter().getFiscalCode(), captured.getDebtorTaxCode());
    assertEquals(po.getAfter().getNav(), captured.getNav());
    assertEquals(po.getAfter().getDueDate(), captured.getDueDate());
    assertEquals(po.getAfter().getAmount(), captured.getAmount());
    assertEquals(debtPosition.getStatus(), captured.getStatus());
    assertEquals(po.getAfter().getPspCode(), captured.getPspCode());
    assertEquals(po.getAfter().getPspTaxCode(), captured.getPspTaxCode());
  }

  @Test
  void ingestPaymentOption_OK_UPDATE() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.u);
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

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setIupd("123456");
    debtPosition.setStatus(PaymentPositionStatus.VALID);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).filterByTaxonomy(any(), any());
    verify(anonymizerClient, times(2)).anonymize(any(AnonymizerModel.class));
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
    assertEquals(ANONIMIZED_RESPONSE.getText(), captured.getDescription());
    assertEquals(po.getAfter().getOrganizationFiscalCode(), captured.getEcTaxCode());
    assertEquals(po.getAfter().getFiscalCode(), captured.getDebtorTaxCode());
    assertEquals(po.getAfter().getNav(), captured.getNav());
    assertEquals(po.getAfter().getDueDate(), captured.getDueDate());
    assertEquals(po.getAfter().getAmount(), captured.getAmount());
    assertEquals(debtPosition.getStatus(), captured.getStatus());
    assertEquals(po.getAfter().getPspCode(), captured.getPspCode());
    assertEquals(po.getAfter().getPspTaxCode(), captured.getPspTaxCode());
  }

  @Test
  void ingestPaymentOption_KO_JSON_PROCESSING_EXCEPTION_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
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
    verify(filterService, never()).filterByTaxCode(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(acknowledgment, never()).nack(any());
  }

  @Test
  void ingestPaymentOption_KO_DEBEZIUM_OPERATION_T_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.t);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).filterByTaxCode(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_DEBEZIUM_OPERATION_R_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.r);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).filterByTaxCode(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_DEBEZIUM_OPERATION_M_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.m);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).filterByTaxCode(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_MESSAGE_NULL_DISCARDED() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.m);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).filterByTaxCode(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
    verify(paymentOptionRepository, never()).findById(anyLong());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_INVALID_PAYMENT_POSITION_STATUS_CREATE_DRAFT_DISCARDED()
      throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setStatus(PaymentPositionStatus.DRAFT);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    doThrow(new FailAndIgnore(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP))
            .when(filterService)
            .filterByStatus(any(), any());


    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).filterByStatus(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(debtPositionRepository).findById(anyLong());
    verify(acknowledgment).acknowledge();
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_REPLICA_SYNC_FAILED_NO_PO() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.empty());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(paymentOptionRepository).findById(anyLong());
  }

  @Test
  void ingestPaymentOption_KO_REPLICA_SYNC_FAILED_ON_DATE() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment);
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW.minus(5L, ChronoUnit.DAYS));
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(paymentOptionRepository).findById(anyLong());
    verify(acknowledgment).nack(any());
    verify(filterService, never()).filterByTaxonomy(any(), any());
    verify(transferRepository, never()).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, never()).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer, never()).sendRTPMessage(any());
    verify(acknowledgment, never()).acknowledge();
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_INVALID_TRANSFER_CATEGORIES() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setIupd("123456");
    debtPosition.setStatus(PaymentPositionStatus.VALID);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of());

    doThrow(new FailAndIgnore(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP))
        .when(filterService)
        .filterByTaxonomy(any(), any());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).filterByTaxonomy(any(), any());
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
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setIupd("123456");
    debtPosition.setStatus(PaymentPositionStatus.VALID);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of());

    doThrow(new FailAndIgnore(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING))
        .when(filterService)
        .filterByTaxonomy(any(), any());

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).filterByTaxonomy(any(), any());
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
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW);
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setIupd("123456");
    debtPosition.setStatus(PaymentPositionStatus.VALID);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    Transfer transfer = new Transfer();
    transfer.setOrganizationFiscalCode("differentOrgFiscalCode");
    when(transferRepository.findByPaymentOptionId(anyLong())).thenReturn(List.of(transfer));

    // test execution
    assertDoesNotThrow(() -> sut.ingestPaymentOption(genericMessage));

    verify(filterService).filterByTaxonomy(any(), any());
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
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
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

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setIupd("123456");
    debtPosition.setStatus(PaymentPositionStatus.VALID);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    when(anonymizerClient.anonymize(any(AnonymizerModel.class))).thenReturn(ANONIMIZED_RESPONSE);

    when(rtpMessageProducer.sendRTPMessage(any(RTPMessage.class))).thenReturn(false);

    // test execution
    try {
      sut.ingestPaymentOption(genericMessage);
    } catch (AppException e) {
      assertEquals(AppError.RTP_MESSAGE_NOT_SENT, e.getAppErrorCode());
    }

    verify(filterService).filterByTaxonomy(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(transferRepository).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, times(2)).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer).sendRTPMessage(any());
    verify(acknowledgment, never()).acknowledge();
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_ERROR_GENERIC() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
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

    PaymentPosition debtPosition = new PaymentPosition();
    debtPosition.setIupd("123456");
    debtPosition.setStatus(PaymentPositionStatus.VALID);
    debtPosition.setServiceType(ServiceType.GPD);
    when(debtPositionRepository.findById(anyLong())).thenReturn(Optional.of(debtPosition));

    when(anonymizerClient.anonymize(any(AnonymizerModel.class))).thenReturn(ANONIMIZED_RESPONSE);

    doThrow(RuntimeException.class).when(rtpMessageProducer).sendRTPMessage(any(RTPMessage.class));

    // test execution
    try {
      sut.ingestPaymentOption(genericMessage);
    } catch (RuntimeException e) {
      assertTrue(true);
    }

    verify(filterService).filterByTaxonomy(any(), any());
    verify(paymentOptionRepository).findById(anyLong());
    verify(transferRepository).findByPaymentOptionId(anyLong());
    verify(anonymizerClient, times(2)).anonymize(any(AnonymizerModel.class));
    verify(rtpMessageProducer).sendRTPMessage(any());
    verify(acknowledgment, never()).acknowledge();
    verify(acknowledgment, never()).nack(any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
  }

  @Test
  void ingestPaymentOption_KO_DB_REPLICA_SYNC_0_retry() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW.minusDays(1));
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));
    sut.ingestPaymentOption(genericMessage);
    verify(redisCacheRepository).setRetryCount(any(), anyInt());
  }

  @Test
  void ingestPaymentOption_KO_DB_REPLICA_SYNC_4_retry() throws JsonProcessingException {
    DataCaptureMessage<PaymentOptionEvent> po = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    Map<String, Object> headers = Map.of(KafkaHeaders.ACKNOWLEDGMENT, acknowledgment, "id", "id");
    Message<String> genericMessage =
        new GenericMessage<>(objectMapper.writeValueAsString(po), headers);

    PaymentOption repoPO = new PaymentOption();
    repoPO.setLastUpdatedDate(DATE_NOW.minusDays(1));
    when(paymentOptionRepository.findById(po.getAfter().getId())).thenReturn(Optional.of(repoPO));
    when(redisCacheRepository.getRetryCount(any())).thenReturn(4);
    sut.ingestPaymentOption(genericMessage);

    verify(redisCacheRepository).deleteRetryCount(any());
  }

  @Test
  void filterPaymentOption_OK() throws JsonProcessingException {
    FilterService filterServiceSpy = spy(new FilterServiceImpl(mock(RedisCacheRepository.class)));
    ReflectionTestUtils.setField(sut, "filterService", filterServiceSpy);

    when(rtpMessageProducer.sendFilteredCdcMessage(any(), any())).thenReturn(true);

    String poBlank = "";
    String poValidCreate = objectMapper.writeValueAsString(EntityUtils.getPaymentOption(DebeziumOperationCode.c));
    String poValidUpdate = objectMapper.writeValueAsString(EntityUtils.getPaymentOption(DebeziumOperationCode.u));
    String poValidDelete = objectMapper.writeValueAsString(getDeletedPaymentOption());
    String poInvalidOperation = objectMapper.writeValueAsString(EntityUtils.getPaymentOption(DebeziumOperationCode.r));

    DataCaptureMessage<PaymentOptionEvent> poInvalidTaxCodeObject = EntityUtils.getPaymentOption(DebeziumOperationCode.c);
    PaymentOptionEvent poEventInvalidTaxCode = poInvalidTaxCodeObject.getAfter();
    poEventInvalidTaxCode.setFiscalCode(ORG_FISCAL_CODE);
    poInvalidTaxCodeObject.setAfter(poEventInvalidTaxCode);
    String poInvalidTaxCode = objectMapper.writeValueAsString(poInvalidTaxCodeObject);

    List<String> messages = List.of(poBlank, poValidCreate, poValidUpdate, poValidDelete, poInvalidOperation, poInvalidTaxCode);

    assertDoesNotThrow(() -> sut.filterPaymentOptions(messages));

    verify(filterServiceSpy, times(5)).filterByDebeziumOperation(any());
    verify(filterServiceSpy, times(4)).filterByTaxCode(any());
    verify(rtpMessageProducer, times(3)).sendFilteredCdcMessage(any(), any());
    verify(deadLetterService, never()).sendToDeadLetter(any());

    ReflectionTestUtils.setField(sut, "filterService", filterService);
  }

  @Test
  void filterPaymentOption_KO_error_send_eventhub() throws JsonProcessingException {
    when(rtpMessageProducer.sendFilteredCdcMessage(any(), any())).thenReturn(false);

    String poValidCreate = objectMapper.writeValueAsString(EntityUtils.getPaymentOption(DebeziumOperationCode.c));
    List<String> messages = List.of(poValidCreate);

    assertDoesNotThrow(() -> sut.filterPaymentOptions(messages));

    verify(rtpMessageProducer, times(1)).sendFilteredCdcMessage(any(), any());
    verify(deadLetterService, times(1)).sendToDeadLetter(any());
  }

  @Test
  void filterPaymentOption_KO_error_unprocessable_message() {
    String invalidMessage = "invalidMessage";
    List<String> messages = List.of(invalidMessage);

    assertDoesNotThrow(() -> sut.filterPaymentOptions(messages));

    verify(rtpMessageProducer, never()).sendFilteredCdcMessage(any(), any());
    verify(deadLetterService, never()).sendToDeadLetter(any());
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
