package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.redis.FlagOptInRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(classes = {FilterServiceImpl.class})
class FilterServiceImplTest {
    private static final String VALID_TRANSFER_CATEGORY = "validTransferCategory";
    private static final String INVALID_TRANSFER_CATEGORY = "invalidTransferCategory";
    private static final String VALID_FISCAL_CODE = "AAAAAA98L12B157A";
    private static final String VALID_PIVA = "01234567890";
    private static final String INVALID_FISCAL_CODE = "invalidFiscalCode";
    private static final long VALID_PAYMENT_OPTION_AMOUNT = 10L;

    @MockBean
    private FlagOptInRepository flagOptInRepository;

    @Autowired
    @InjectMocks
    private FilterServiceImpl sut;

    // Verify PaymentPositionStatus
    @Test
    void isValidPaymentOptionForRTP_OK_VALID_OPERATION_C() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.VALID, VALID_FISCAL_CODE, DebeziumOperationCode.c)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_VALID_OPERATION_U() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.VALID, VALID_FISCAL_CODE, DebeziumOperationCode.u)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_PAID_OPERATION_C() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.PAID, VALID_FISCAL_CODE, DebeziumOperationCode.c)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_PAID_OPERATION_U() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.PAID, VALID_FISCAL_CODE, DebeziumOperationCode.u)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_PARTIALLY_PAID_OPERATION_C() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.PARTIALLY_PAID, VALID_FISCAL_CODE, DebeziumOperationCode.c)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_PARTIALLY_PAID_OPERATION_U() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.PARTIALLY_PAID, VALID_FISCAL_CODE, DebeziumOperationCode.u)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_EXPIRED_PAID_OPERATION_C() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.EXPIRED, VALID_FISCAL_CODE, DebeziumOperationCode.c)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_EXPIRED_PAID_OPERATION_U() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.EXPIRED, VALID_FISCAL_CODE, DebeziumOperationCode.u)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_INVALID_PAID_OPERATION_C() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.INVALID, VALID_FISCAL_CODE, DebeziumOperationCode.c)));
    }

    @Test
    void isValidPaymentOptionForRTP_OK_INVALID_PAID_OPERATION_U() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.INVALID, VALID_FISCAL_CODE, DebeziumOperationCode.u)));
    }

    @Test
    void isValidPaymentOptionForRTP_KO_DRAFT_OPERATION_C() {
        try{
            sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.DRAFT, VALID_FISCAL_CODE, DebeziumOperationCode.c));
        } catch (AppException e){
            assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void isValidPaymentOptionForRTP_OK_DRAFT_OPERATION_U() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.DRAFT, VALID_FISCAL_CODE, DebeziumOperationCode.u)));
    }

    @Test
    void isValidPaymentOptionForRTP_KO_PUBLISHED_OPERATION_C() {
        try{
            sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.PUBLISHED, VALID_FISCAL_CODE, DebeziumOperationCode.c));
        } catch (AppException e){
            assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void isValidPaymentOptionForRTP_OK_PUBLISHED_OPERATION_U() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.PUBLISHED, VALID_FISCAL_CODE, DebeziumOperationCode.u)));
    }

    @Test
    void isValidPaymentOptionForRTP_KO_REPORTED_OPERATION_C() {
        try{
            sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.REPORTED, VALID_FISCAL_CODE, DebeziumOperationCode.c));
        } catch (AppException e){
            assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void isValidPaymentOptionForRTP_KO_REPORTED_OPERATION_U() {
        try{
            sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.REPORTED, VALID_FISCAL_CODE, DebeziumOperationCode.u));
        } catch (AppException e){
            assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void isValidPaymentOptionForRTP_KO_AFTER_VALUES_NULL() {
        try{
            sut.isValidPaymentOptionForRTPOrElseThrow(new DataCaptureMessage<PaymentOptionEvent>());
        } catch (AppException e){
            assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void isValidPaymentOptionForRTP_KO_PAYMENT_POSITION_STATUS_NULL() {
        try{
            sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(null, VALID_FISCAL_CODE, DebeziumOperationCode.u));
        } catch (AppException e){
            assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    // Verify FiscalCodeFilter
    @Test
    void isValidPaymentOptionForRTP_OK_VALID_FISCAL_CODE() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.VALID, VALID_FISCAL_CODE, DebeziumOperationCode.c)));
    }
    @Test
    void isValidPaymentOptionForRTP_OK_VALID_PIVA() {
        assertDoesNotThrow(() -> sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.VALID, VALID_PIVA, DebeziumOperationCode.c)));
    }
    @Test
    void isValidPaymentOptionForRTP_OK_INVALID_FISCAL_CODE() {
        try{
            sut.isValidPaymentOptionForRTPOrElseThrow(getDataCapureMessagePaymentOption(PaymentPositionStatus.VALID, INVALID_FISCAL_CODE, DebeziumOperationCode.c));
        } catch (AppException e){
            assertEquals(AppError.TAX_CODE_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    // Verify Transfers
    @Test
    void hasValidTransferCategoriesOrElseThrow_OK(){
        assertDoesNotThrow(() -> sut.hasValidTransferCategoriesOrElseThrow(getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT), getTransferList(VALID_TRANSFER_CATEGORY, VALID_TRANSFER_CATEGORY)));
    }
    @Test
    void hasValidTransferCategoriesOrElseThrow_KO_INVALID_AMOUNT(){
        try{
            sut.hasValidTransferCategoriesOrElseThrow(getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT/2), getTransferList(VALID_TRANSFER_CATEGORY, VALID_TRANSFER_CATEGORY));
        } catch (AppException e){
            assertEquals(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING, e.getAppErrorCode());
        }
    }
    @Test
    void hasValidTransferCategoriesOrElseThrow_KO_INVALID_ONE_TRANSFER_CATEGORY(){
        try{
            sut.hasValidTransferCategoriesOrElseThrow(getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT), getTransferList(VALID_TRANSFER_CATEGORY, INVALID_TRANSFER_CATEGORY));
        } catch (AppException e){
            assertEquals(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }
    @Test
    void hasValidTransferCategoriesOrElseThrow_KO_INVALID_BOTH_TRANSFER_CATEGORY(){
        try{
            sut.hasValidTransferCategoriesOrElseThrow(getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT), getTransferList(INVALID_TRANSFER_CATEGORY, INVALID_TRANSFER_CATEGORY));
        } catch (AppException e){
            assertEquals(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }


    private DataCaptureMessage<PaymentOptionEvent> getDataCapureMessagePaymentOption(PaymentPositionStatus paymentPositionStatus, String fiscalCode, DebeziumOperationCode debeziumOperationCode) {
        return DataCaptureMessage.<PaymentOptionEvent>builder()
                .before(null)
                .after(PaymentOptionEvent.builder()
                        .paymentPositionStatus(paymentPositionStatus)
                        .fiscalCode(fiscalCode)
                        .build())
                .op(debeziumOperationCode)
                .build();
    }

    private PaymentOptionEvent getPaymentOption(long amount) {
        return PaymentOptionEvent.builder().amount(amount).build();
    }

    private List<Transfer> getTransferList(String transferCategory1, String transferCategory2){
        Transfer transfer1 = new Transfer();
        transfer1.setAmount(VALID_PAYMENT_OPTION_AMOUNT/2);
        transfer1.setCategory(transferCategory1);
        Transfer transfer2 = new Transfer();
        transfer2.setAmount(VALID_PAYMENT_OPTION_AMOUNT/2);
        transfer2.setCategory(transferCategory2);
        return List.of(transfer1,transfer2);
    }
}
