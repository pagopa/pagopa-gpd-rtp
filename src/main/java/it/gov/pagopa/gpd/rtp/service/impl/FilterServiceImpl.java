package it.gov.pagopa.gpd.rtp.service.impl;

import static it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository.KEY;

import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.FailAndIgnore;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import java.util.List;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FilterServiceImpl implements FilterService {

  private final List<String> validTransferCategories;

  private final RedisCacheRepository redisCacheRepository;

  @Autowired
  public FilterServiceImpl(
      @Value("#{'${gpd.rtp.ingestion.service.transfer.categories}'.split(',')}")
          List<String> validTransferCategories,
      RedisCacheRepository redisCacheRepository) {
    this.redisCacheRepository = redisCacheRepository;
    this.validTransferCategories = validTransferCategories;
  }

  @Override
  public void isValidPaymentOptionForRTPOrElseThrow(
      DataCaptureMessage<PaymentOptionEvent> paymentOption) {
    PaymentOptionEvent valuesAfter = paymentOption.getAfter();

    // Check payment position status
    if (isInvalidPaymentPositionStatus(valuesAfter, paymentOption.getOp()))
      throw new FailAndIgnore(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP);

    // Debtor Tax Code Validation
    if (valuesAfter.getFiscalCode().equals(valuesAfter.getOrganizationFiscalCode())
        || isInvalidFiscalCode(valuesAfter.getFiscalCode())) {
      throw new FailAndIgnore(AppError.TAX_CODE_NOT_VALID_FOR_RTP);
    }

    // Check flag opt-in
    var hasRtpEnabled = isPresent(valuesAfter.getOrganizationFiscalCode());
    if (!hasRtpEnabled) {
      throw new FailAndIgnore(AppError.EC_NOT_ENABLED_FOR_RTP);
    }
  }

  public boolean isPresent(String idDominio) {
    return redisCacheRepository.isCacheUpdated()
        && Boolean.TRUE.equals(redisCacheRepository.getFlags().isMember(KEY, idDominio));
  }

  @Override
  public void hasValidTransferCategoriesOrElseThrow(
      PaymentOptionEvent paymentOption, List<Transfer> transferList) {
    if (!transferList.parallelStream()
        .allMatch(transfer -> this.validTransferCategories.contains(transfer.getCategory()))) {
      throw new FailAndIgnore(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP);
    }
    long totalTransfersAmount =
        transferList.stream()
            .reduce(0L, (subtotal, element) -> subtotal + element.getAmount(), Long::sum);
    if (totalTransfersAmount != paymentOption.getAmount()) {
      throw new FailAndIgnore(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING);
    }
  }

  private boolean isInvalidPaymentPositionStatus(
      PaymentOptionEvent valuesAfter, DebeziumOperationCode debeziumOperationCode) {
    return valuesAfter == null
        || valuesAfter.getPaymentPositionStatus() == null
        || isPaymentPositionCreateWithStatusDraftOrPublished(
            debeziumOperationCode, valuesAfter.getPaymentPositionStatus())
        || valuesAfter.getPaymentPositionStatus().equals(PaymentPositionStatus.REPORTED);
  }

  private boolean isPaymentPositionCreateWithStatusDraftOrPublished(
      DebeziumOperationCode debeziumOperationCode, PaymentPositionStatus paymentPositionStatus) {
    return debeziumOperationCode.equals(DebeziumOperationCode.c)
        && (paymentPositionStatus.equals(PaymentPositionStatus.DRAFT)
            || paymentPositionStatus.equals(PaymentPositionStatus.PUBLISHED));
  }

  private boolean isInvalidFiscalCode(String fiscalCode) {
    if (fiscalCode != null && !fiscalCode.isEmpty()) {
      Pattern patternCF =
          Pattern.compile(
              "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
      Pattern patternPIVA = Pattern.compile("^\\d{11}$");

      return !(patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find());
    }
    return true;
  }
}
