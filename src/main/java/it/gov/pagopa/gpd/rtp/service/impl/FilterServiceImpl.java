package it.gov.pagopa.gpd.rtp.service.impl;

import static it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository.KEY;

import it.gov.pagopa.gpd.rtp.entity.PaymentPosition;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.entity.enumeration.ServiceType;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.FailAndIgnore;
import it.gov.pagopa.gpd.rtp.exception.FailAndPostpone;
import it.gov.pagopa.gpd.rtp.repository.RedisCacheRepository;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class FilterServiceImpl implements FilterService {

  public static final String TRANSFER_CATEGORIES = "transferCategories";

  private final RedisCacheRepository redisCacheRepository;

  @Autowired
  public FilterServiceImpl(RedisCacheRepository redisCacheRepository) {
    this.redisCacheRepository = redisCacheRepository;
  }

  @Override
  public void filterByTaxCode(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
    PaymentOptionEvent valuesAfter = paymentOption.getAfter();

    // Debtor Tax Code Validation
    if (valuesAfter.getFiscalCode().equals(valuesAfter.getOrganizationFiscalCode())) {
      throw new FailAndIgnore(AppError.TAX_CODE_NOT_VALID_FOR_RTP);
    }
  }

  @Override
  public void filterByOptInFlag(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
    PaymentOptionEvent valuesAfter = paymentOption.getAfter();

    // Check flag opt-in
    var hasRtpEnabled = isPresent(valuesAfter.getOrganizationFiscalCode());
    if (!hasRtpEnabled) {
      throw new FailAndIgnore(AppError.EC_NOT_ENABLED_FOR_RTP);
    }
  }

  @Override
  public void filterByStatus(PaymentPosition debtPosition, DebeziumOperationCode operation) {

    // Check payment position status
    if (isInvalidPaymentPositionStatus(debtPosition, operation))
      throw new FailAndIgnore(AppError.PAYMENT_POSITION_STATUS_NOT_VALID_FOR_RTP);
  }

  @Override
  public void filterByServiceType(PaymentPosition debtPosition) {

    // check service type
    boolean pdIsGpd = ServiceType.GPD.equals(debtPosition.getServiceType());
    boolean pdIsAca = ServiceType.ACA.equals(debtPosition.getServiceType());
    boolean pdIsPaCreate = debtPosition.getIupd().startsWith("ACA_");
    boolean pdIsAcaGpd = pdIsAca && !pdIsPaCreate;
    if (!(pdIsGpd || pdIsAcaGpd)) {
      throw new FailAndIgnore(AppError.PAYMENT_POSITION_TYPE_NOT_VALID_FOR_RTP);
    }
  }

  @Override
  public void filterByTaxonomy(PaymentOptionEvent paymentOption, List<Transfer> transferList) {
    List<String> transferCategories = transferList.stream().map(Transfer::getCategory).toList();

    MDC.put(TRANSFER_CATEGORIES, String.join(",", transferCategories));

    if (transferCategories.parallelStream()
        .anyMatch(elem -> taxonomyStartsWithOptOut(elem) && taxonomyIsValid(elem))) {
      throw new FailAndIgnore(AppError.TRANSFERS_CATEGORIES_NOT_VALID_FOR_RTP);
    }

    long totalTransfersAmount =
        transferList.stream()
            .reduce(0L, (subtotal, element) -> subtotal + element.getAmount(), Long::sum);
    if (totalTransfersAmount != paymentOption.getAmount()) {
      throw new FailAndPostpone(AppError.TRANSFERS_TOTAL_AMOUNT_NOT_MATCHING);
    }
  }

  private static boolean taxonomyStartsWithOptOut(String elem) {
    return elem.startsWith("6/") || elem.startsWith("7/") || elem.startsWith("8/");
  }

  private static boolean taxonomyIsValid(String elem) {
    String taxonomy = getTaxonomyValue(elem);
    return taxonomy != null && taxonomy.matches("\\d{2}\\d{2}\\d{3}\\w{2}");
  }

  /**
   * Extracts and returns the taxonomy value from the given element string. examples: 9/9182ABC/ ->
   * 9182ABC 9182ABC -> 9182ABC
   *
   * @param elem the input string containing taxonomy information, expected to be in the format
   *     "category/taxonomy".
   * @return the taxonomy part of the input string if available, otherwise returns the original
   *     string.
   */
  private static String getTaxonomyValue(String elem) {
    if (elem == null) {
      return null;
    }

    String[] split = elem.split("/");

    // 12324    split[0]
    // 9/23423  split[1]
    // /12324   split[1]
    // /23423/  split[1]
    // 1212/    split[0]
    // 9/23423/ split[1]

    return split.length > 1 ? split[1] : split[0];
  }

  /**
   * @param paymentPosition
   * @param debeziumOperationCode
   * @return Restituisce true se lo stato della payment position è considerato non valido per
   *     proseguire. Condizioni che rendono lo stato non valido: - l'evento è una creazione
   *     (Debezium code c) e lo stato è DRAFT o PUBLISHED. - lo stato è REPORTED.
   */
  private boolean isInvalidPaymentPositionStatus(
      PaymentPosition paymentPosition, DebeziumOperationCode debeziumOperationCode) {
    return paymentPosition == null
        || paymentPosition.getStatus() == null
        || isPaymentPositionCreateWithStatusDraftOrPublished(
            debeziumOperationCode, paymentPosition.getStatus())
        || paymentPosition.getStatus().equals(PaymentPositionStatus.REPORTED);
  }

  private boolean isPaymentPositionCreateWithStatusDraftOrPublished(
      DebeziumOperationCode debeziumOperationCode, PaymentPositionStatus paymentPositionStatus) {
    return debeziumOperationCode.equals(DebeziumOperationCode.c)
        && (paymentPositionStatus.equals(PaymentPositionStatus.DRAFT)
            || paymentPositionStatus.equals(PaymentPositionStatus.PUBLISHED));
  }

  private boolean isPresent(String idDominio) {
    return redisCacheRepository.isCacheUpdated()
        && Boolean.TRUE.equals(redisCacheRepository.getFlags().isMember(KEY, idDominio));
  }
}
