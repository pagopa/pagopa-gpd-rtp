package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.entity.PaymentPosition;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.entity.enumeration.ServiceType;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.SetOperations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(classes = {FilterServiceImpl.class})
class FilterServiceImplTest {
    private static final String VALID_TRANSFER_CATEGORY = "9/0201102IM/";
    private static final String INVALID_TRANSFER_CATEGORY = "invalidTransferCategory";
    private static final long VALID_PAYMENT_OPTION_AMOUNT = 10L;

    @MockBean
    private RedisCacheRepository redisCacheRepository;

    @Autowired
    @InjectMocks
    private FilterServiceImpl sut;


    @Test
    void filterByTaxCode_OK() {
        var po = new DataCaptureMessage<PaymentOptionEvent>();
        po.setAfter(new PaymentOptionEvent());
        po.getAfter().setFiscalCode("fiscalCode");
        po.getAfter().setOrganizationFiscalCode("organizationFiscalCode");
        assertDoesNotThrow(
                () ->
                        sut.filterByTaxCode(po)
        );
    }

    @Test
    void filterByTaxCode_KO() {
        var po = new DataCaptureMessage<PaymentOptionEvent>();
        po.setAfter(new PaymentOptionEvent());
        po.getAfter().setFiscalCode("organizationFiscalCode");
        po.getAfter().setOrganizationFiscalCode("organizationFiscalCode");
        try {
            sut.filterByTaxCode(po);
        } catch (AppException e) {
            assertEquals(AppError.TAX_CODE_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void filterByOptInFlag_OK() {
        when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
        SetOperations mock = Mockito.mock(SetOperations.class);
        when(mock.isMember(anyString(), anyString())).thenReturn(true);
        when(redisCacheRepository.getFlags()).thenReturn(mock);


        var po = new DataCaptureMessage<PaymentOptionEvent>();
        po.setAfter(new PaymentOptionEvent());
        po.getAfter().setFiscalCode("fiscalCode");
        po.getAfter().setOrganizationFiscalCode("organizationFiscalCode");
        assertDoesNotThrow(
                () ->
                        sut.filterByOptInFlag(po)
        );
    }

    @Test
    void filterByOptInFlag_KO() {
        when(redisCacheRepository.isCacheUpdated()).thenReturn(true);
        SetOperations mock = Mockito.mock(SetOperations.class);
        when(mock.isMember(anyString(), anyString())).thenReturn(false);
        when(redisCacheRepository.getFlags()).thenReturn(mock);


        var po = new DataCaptureMessage<PaymentOptionEvent>();
        po.setAfter(new PaymentOptionEvent());
        po.getAfter().setFiscalCode("organizationFiscalCode");
        po.getAfter().setOrganizationFiscalCode("organizationFiscalCode");
        try {
            sut.filterByOptInFlag(po);
        } catch (AppException e) {
            assertEquals(AppError.EC_NOT_ENABLED_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void filterByStatus_OK_OPERATION_C() {

        var pd = new PaymentPosition();
        pd.setStatus(PaymentPositionStatus.VALID);
        assertDoesNotThrow(
                () -> sut.filterByStatus(pd, DebeziumOperationCode.c)
        );
    }

    @Test
    void filterByStatus_OK_OPERATION_U() {

        var pd = new PaymentPosition();
        pd.setStatus(PaymentPositionStatus.VALID);
        assertDoesNotThrow(
                () -> sut.filterByStatus(pd, DebeziumOperationCode.u)
        );
    }


    @Test
    void filterByStatus_KO() {

        var pd = new PaymentPosition();
        pd.setStatus(PaymentPositionStatus.REPORTED);
        try {
            sut.filterByStatus(pd, DebeziumOperationCode.c);
        } catch (AppException e) {
            assertEquals(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }


    @Test
    void filterByServiceType_OK_GPD() {

        var pd = new PaymentPosition();
        pd.setServiceType(ServiceType.GPD);
        pd.setIupd("someIupdValue");
        assertDoesNotThrow(
                () -> sut.filterByServiceType(pd)
        );
    }

    @Test
    void filterByServiceType_OK_ACA_GPD() {

        var pd = new PaymentPosition();
        pd.setServiceType(ServiceType.ACA);
        pd.setIupd("someIupdValue");
        assertDoesNotThrow(
                () -> sut.filterByServiceType(pd)
        );
    }

    @Test
    void filterByServiceType_KO_ACA() {

        var pd = new PaymentPosition();
        pd.setServiceType(ServiceType.ACA);
        pd.setIupd("ACA_someIupdValue");
        try {
            sut.filterByServiceType(pd);
        } catch (AppException e) {
            assertEquals(AppError.PAYMENT_POSITION_TYPE_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }


    // Verify Transfers
    @Test
    void filterByTaxonomy_OK() {
        assertDoesNotThrow(
                () ->
                        sut.filterByTaxonomy(
                                getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT),
                                getTransferList(VALID_TRANSFER_CATEGORY, VALID_TRANSFER_CATEGORY)));
    }

    @Test
    void filterByTaxonomy_KO_INVALID_AMOUNT() {
        try {
            sut.filterByTaxonomy(
                    getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT / 2),
                    getTransferList(VALID_TRANSFER_CATEGORY, VALID_TRANSFER_CATEGORY));
        } catch (AppException e) {
            assertEquals(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING, e.getAppErrorCode());
        }
    }

    @Test
    void filterByTaxonomy_KO_OPTOUT_CATEGORY() {
        try {
            sut.filterByTaxonomy(
                    getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT / 2),
                    getTransferList("7/0201102IM/", VALID_TRANSFER_CATEGORY));
        } catch (AppException e) {
            assertEquals(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void filterByTaxonomy_KO_INVALID_ONE_TRANSFER_CATEGORY() {
        try {
            sut.filterByTaxonomy(
                    getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT),
                    getTransferList(VALID_TRANSFER_CATEGORY, INVALID_TRANSFER_CATEGORY));
        } catch (AppException e) {
            assertEquals(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
    }

    @Test
    void filterByTaxonomy_KO_INVALID_BOTH_TRANSFER_CATEGORY() {
        try {
            sut.filterByTaxonomy(
                    getPaymentOption(VALID_PAYMENT_OPTION_AMOUNT),
                    getTransferList(INVALID_TRANSFER_CATEGORY, INVALID_TRANSFER_CATEGORY));
        } catch (AppException e) {
            assertEquals(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP, e.getAppErrorCode());
        }
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
