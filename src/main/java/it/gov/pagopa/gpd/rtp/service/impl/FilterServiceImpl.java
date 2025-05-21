package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.entity.redis.FlagOptIn;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.redis.FlagOptInRepository;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.regex.Pattern;

@Service
@Slf4j
public class FilterServiceImpl implements FilterService {

    private final FlagOptInRepository flagOptInRepository;

    @Autowired
    public FilterServiceImpl(FlagOptInRepository flagOptInRepository) {
        this.flagOptInRepository = flagOptInRepository;
    }

    @Override
    public void isValidPaymentOptionForRTP(DataCaptureMessage<PaymentOption> paymentOption) {
        PaymentOption valuesBefore = paymentOption.getBefore();
        PaymentOption valuesAfter = paymentOption.getAfter();

        // Check payment position status
        if (!verifyPaymentPositionStatus(valuesBefore, valuesAfter))
            throw new AppException(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP);

        // Debtor Tax Code Validation
        if (valuesAfter == null || valuesAfter.getFiscalCode().equals(valuesAfter.getOrganizationFiscalCode()) || isInvalidFiscalCode(valuesAfter.getFiscalCode())) {
            throw new AppException(AppError.TAX_CODE_NOT_VALID_FOR_RTP);
        }

        // Check flag opt-in
        Optional<FlagOptIn> flagOptInOptional = flagOptInRepository.findById(valuesAfter.getFiscalCode());
        if (flagOptInOptional.isEmpty() || !flagOptInOptional.get().isFlagOptIn()) {
            throw new AppException(AppError.EC_NOT_ENABLED_FOR_RTP);
        }
        // TODO se Flag rtp_cache_created_at è null o troppo vecchio (+2 days) chiama l’api RTP per aggiornare la cache (vedi paragrafo su update cache)
    }

    private static boolean verifyPaymentPositionStatus(PaymentOption valuesBefore, PaymentOption valuesAfter) {
        if (valuesBefore != null && (
                valuesBefore.getPaymentPositionStatus().equals(PaymentPositionStatus.VALID) ||
                        valuesBefore.getPaymentPositionStatus().equals(PaymentPositionStatus.PARTIALLY_PAID)
        )) {
            return true;
        }
        if (valuesAfter != null && (
                valuesAfter.getPaymentPositionStatus().equals(PaymentPositionStatus.VALID) ||
                        valuesAfter.getPaymentPositionStatus().equals(PaymentPositionStatus.PARTIALLY_PAID)
        )) {
            return true;
        }
        return false;
    }

    private boolean isInvalidFiscalCode(String fiscalCode) {
        if (fiscalCode != null && !fiscalCode.isEmpty()) {
            Pattern patternCF =
                    Pattern.compile(
                            "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$"); // TODO VERIFICA CHECKSUM?
            Pattern patternPIVA = Pattern.compile("/^[0-9]{11}$/");

            return !(patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find());
        }

        return true;
    }
}
