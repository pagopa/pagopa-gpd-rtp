package it.gov.pagopa.gpd.rtp.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.SetOperations;

@SpringBootTest(classes = {FilterServiceImpl.class})
class FilterServiceImplTest {
  private static final String VALID_TRANSFER_CATEGORY = "9/0201102IM/";
  private static final String INVALID_TRANSFER_CATEGORY = "invalidTransferCategory";
  private static final String VALID_FISCAL_CODE = "AAAAAA98L12B157A";
  private static final String VALID_PIVA = "01234567890";
  private static final String INVALID_FISCAL_CODE = "invalidFiscalCode";
  private static final long VALID_PAYMENT_OPTION_AMOUNT = 10L;

  @MockBean private RedisCacheRepository redisCacheRepository;

  @Autowired @InjectMocks private FilterServiceImpl sut;

  // Verify PaymentPositionStatus
  @Test
  void isValidPaymentOptionForRTP_OK_VALID_OPERATION_C() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.VALID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.c)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_VALID_OPERATION_U() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.VALID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.u)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_PAID_OPERATION_C() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.PAID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.c)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_PAID_OPERATION_U() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.PAID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.u)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_PARTIALLY_PAID_OPERATION_C() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.PARTIALLY_PAID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.c)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_PARTIALLY_PAID_OPERATION_U() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.PARTIALLY_PAID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.u)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_EXPIRED_PAID_OPERATION_C() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.EXPIRED,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.c)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_EXPIRED_PAID_OPERATION_U() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.EXPIRED,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.u)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_INVALID_PAID_OPERATION_C() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.INVALID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.c)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_INVALID_PAID_OPERATION_U() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.INVALID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.u)));
  }

  @Test
  void isValidPaymentOptionForRTP_KO_DRAFT_OPERATION_C() {
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              PaymentPositionStatus.DRAFT, VALID_PIVA, VALID_FISCAL_CODE, DebeziumOperationCode.c));
    } catch (AppException e) {
      assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void isValidPaymentOptionForRTP_OK_DRAFT_OPERATION_U() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.DRAFT,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.u)));
  }

  @Test
  void isValidPaymentOptionForRTP_KO_PUBLISHED_OPERATION_C() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              PaymentPositionStatus.PUBLISHED,
              VALID_PIVA,
              VALID_FISCAL_CODE,
              DebeziumOperationCode.c));
    } catch (AppException e) {
      assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void isValidPaymentOptionForRTP_OK_PUBLISHED_OPERATION_U() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.PUBLISHED,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.u)));
  }

  @Test
  void isValidPaymentOptionForRTP_KO_REPORTED_OPERATION_C() {
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              PaymentPositionStatus.REPORTED,
              VALID_PIVA,
              VALID_FISCAL_CODE,
              DebeziumOperationCode.c));
    } catch (AppException e) {
      assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void isValidPaymentOptionForRTP_KO_REPORTED_OPERATION_U() {
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              PaymentPositionStatus.REPORTED,
              VALID_PIVA,
              VALID_FISCAL_CODE,
              DebeziumOperationCode.u));
    } catch (AppException e) {
      assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void isValidPaymentOptionForRTP_KO_AFTER_VALUES_NULL() {
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(new DataCaptureMessage<PaymentOptionEvent>());
    } catch (AppException e) {
      assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void isValidPaymentOptionForRTP_KO_PAYMENT_POSITION_STATUS_NULL() {
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              null, VALID_PIVA, VALID_FISCAL_CODE, DebeziumOperationCode.u));
    } catch (AppException e) {
      assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  // Verify FiscalCodeFilter
  @Test
  void isValidPaymentOptionForRTP_OK_VALID_PIVA() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    assertDoesNotThrow(
        () ->
            sut.isValidPaymentOptionForRTPOrElseThrow(
                getDataCaptureMessagePaymentOption(
                    PaymentPositionStatus.VALID,
                    VALID_PIVA,
                    VALID_FISCAL_CODE,
                    DebeziumOperationCode.c)));
  }

  @Test
  void isValidPaymentOptionForRTP_OK_INVALID_FISCAL_CODE() {
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              PaymentPositionStatus.VALID,
              VALID_PIVA,
              INVALID_FISCAL_CODE,
              DebeziumOperationCode.c));
    } catch (AppException e) {
      assertEquals(AppError.TAX_CODE_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  // Verify Transfers
  @Test
  void hasValidTransferCategoriesOrElseThrow_OK() {
    assertDoesNotThrow(
        () ->
            sut.hasValidTransferCategoriesOrElseThrow(
                getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT),
                getTransferList(VALID_TRANSFER_CATEGORY, VALID_TRANSFER_CATEGORY)));
  }

  @Test
  void hasValidTransferCategoriesOrElseThrow_KO_INVALID_AMOUNT() {
    try {
      sut.hasValidTransferCategoriesOrElseThrow(
          getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT / 2),
          getTransferList(VALID_TRANSFER_CATEGORY, VALID_TRANSFER_CATEGORY));
    } catch (AppException e) {
      assertEquals(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING, e.getAppErrorCode());
    }
  }

  @Test
  void hasValidTransferCategoriesOrElseThrow_KO_INVALID_ONE_TRANSFER_CATEGORY() {
    try {
      sut.hasValidTransferCategoriesOrElseThrow(
          getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT),
          getTransferList(VALID_TRANSFER_CATEGORY, INVALID_TRANSFER_CATEGORY));
    } catch (AppException e) {
      assertEquals(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void hasValidTransferCategoriesOrElseThrow_KO_INVALID_BOTH_TRANSFER_CATEGORY() {
    try {
      sut.hasValidTransferCategoriesOrElseThrow(
          getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT),
          getTransferList(INVALID_TRANSFER_CATEGORY, INVALID_TRANSFER_CATEGORY));
    } catch (AppException e) {
      assertEquals(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void isValidPaymentOptionForRTP_KO_FLAG_OPT_IN_NOT_ENABLED() {
    when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(false);
    when(redisCacheRepository.getFlags()).thenReturn(mock);
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              PaymentPositionStatus.VALID, VALID_PIVA, VALID_FISCAL_CODE, DebeziumOperationCode.c));
    } catch (AppException e) {
      assertEquals(AppError.EC_NOT_ENABLED_FOR_RTP, e.getAppErrorCode());
    }
  }

  @Test
  void isValidPaymentOptionForRTP_CACHE_NOT_UPDATED() {
    when(redisCacheRepository.isCacheUpdated())
        .thenThrow(new AppException(AppError.REDIS_CACHE_NOT_UPDATED));
    try {
      sut.isValidPaymentOptionForRTPOrElseThrow(
          getDataCaptureMessagePaymentOption(
              PaymentPositionStatus.VALID, VALID_PIVA, VALID_FISCAL_CODE, DebeziumOperationCode.c));
    } catch (AppException e) {
      assertEquals(AppError.REDIS_CACHE_NOT_UPDATED, e.getAppErrorCode());
    }
  }

  private DataCaptureMessage<PaymentOptionEvent> getDataCaptureMessagePaymentOption(
      PaymentPositionStatus paymentPositionStatus,
      String orgFiscalCode,
      String fiscalCode,
      DebeziumOperationCode debeziumOperationCode) {
    return DataCaptureMessage.<PaymentOptionEvent>builder()
        .before(null)
        .after(
            PaymentOptionEvent.builder()
                .paymentPositionStatus(paymentPositionStatus)
                .organizationFiscalCode(orgFiscalCode)
                .fiscalCode(fiscalCode)
                .build())
        .op(debeziumOperationCode)
        .build();
  }

  private PaymentOptionEvent getPaymentOption(long amount) {
    return PaymentOptionEvent.builder().amount(amount).build();
  }

  private List<Transfer> getTransferList(String transferCategory1, String transferCategory2) {
    Transfer transfer1 = new Transfer();
    transfer1.setAmount(VALID_PAYMENT_OPTION_AMOUNT / 2);
    transfer1.setCategory(transferCategory1);
    Transfer transfer2 = new Transfer();
    transfer2.setAmount(VALID_PAYMENT_OPTION_AMOUNT / 2);
    transfer2.setCategory(transferCategory2);
    return List.of(transfer1, transfer2);
  }
}
