package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.redis.FlagOptInRepository;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FilterServiceImpl implements FilterService {

    private final List<String> validTransferCategories;

    private final FlagOptInRepository flagOptInRepository;

    @Autowired
    public FilterServiceImpl(FlagOptInRepository flagOptInRepository,
                             @Value("#{'${gpd.rtp.ingestion.service.transfer.categories}'.split(',')}") List<String> validTransferCategories) {
        this.flagOptInRepository = flagOptInRepository;
        this.validTransferCategories = validTransferCategories;
    }

    @Override
    public void isValidPaymentOptionForRTPOrElseThrow(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
        PaymentOptionEvent valuesAfter = paymentOption.getAfter();

        // Check payment position status
        if (isInvalidPaymentPositionStatus(valuesAfter, paymentOption.getOp()))
            throw new AppException(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP);

        // Debtor Tax Code Validation
        if (valuesAfter.getFiscalCode().equals(valuesAfter.getOrganizationFiscalCode()) || isInvalidFiscalCode(valuesAfter.getFiscalCode())) {
            throw new AppException(AppError.TAX_CODE_NOT_VALID_FOR_RTP);
        }

        // Check flag opt-in
        // TODO uncomment when flag opt-in funcionality is ready
      /*  Optional<FlagOptIn> flagOptInOptional = flagOptInRepository.findById(valuesAfter.getFiscalCode());
        if (flagOptInOptional.isEmpty() || !flagOptInOptional.get().isFlagOptIn()) {
            throw new AppException(AppError.EC_NOT_ENABLED_FOR_RTP);
        }*/
        // TODO se Flag rtp_cache_created_at è null o troppo vecchio (+2 days) chiama l’api RTP per aggiornare la cache (vedi paragrafo su update cache)
    }

    private static boolean isInvalidPaymentPositionStatus(PaymentOptionEvent valuesAfter, DebeziumOperationCode debeziumOperationCode) {
        return valuesAfter == null ||
                (debeziumOperationCode.equals(DebeziumOperationCode.c) &&
                        (valuesAfter.getPaymentPositionStatus().equals(PaymentPositionStatus.DRAFT) ||
                                valuesAfter.getPaymentPositionStatus().equals(PaymentPositionStatus.PUBLISHED))) ||
                valuesAfter.getPaymentPositionStatus().equals(PaymentPositionStatus.REPORTED);
    }

    private boolean isInvalidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            Pattern patternCF =
                    Pattern.compile(
                            "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
            Pattern patternPIVA = Pattern.compile("/^[0-9]{11}$/");

            return !(patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find());
        }

        return true;
    }

    @Override
    public void hasValidTransferCategoriesOrElseThrow(PaymentOptionEvent paymentOption, List<Transfer> transferList) {
        if (!transferList.parallelStream().allMatch(transfer -> this.validTransferCategories.contains(transfer.getCategory()))) {
            throw new AppException(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP);
        }
        long totalTransfersAmount = transferList.stream().reduce(0L, (subtotal, element) -> subtotal + element.getAmount(), Long::sum);
        if (totalTransfersAmount != paymentOption.getAmount()) {
            throw new AppException(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING);
        }
    }
}
