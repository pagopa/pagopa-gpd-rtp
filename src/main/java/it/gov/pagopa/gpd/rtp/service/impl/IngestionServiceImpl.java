package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import it.gov.pagopa.gpd.rtp.events.model.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.events.model.entity.enumeration.PaymentPositionStatus;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {
    private final ObjectMapper objectMapper;
    private final RTPMessageProducer rtpMessageProducer;

    private static final String LOG_PREFIX = "[GPDxRTP]";

    @Value("${gpd.rtp.ingestion.service.maxRetries}")
    private int maxRetries;
    // TODO category with 9/.../ or remove from transfer's string?

    private List<String> validTransferCategories;

    @Autowired
    public IngestionServiceImpl(
            ObjectMapper objectMapper,
            RTPMessageProducer rtpMessageProducer,
            @Value("#{'${gpd.rtp.ingestion.service.transfer.categories}'.split(',')}") List<String> validTransferCategories
            ) {
        this.objectMapper = objectMapper;
        this.rtpMessageProducer = rtpMessageProducer;
        this.validTransferCategories = validTransferCategories;
    }


    // TODO Detach filter logic from main method to another service
    public void ingestPaymentOptions(List<Message<String>> messages) throws Exception {
        log.debug(
                "PaymentOption ingestion called at {} for payment options with events list size {}",
                LocalDateTime.now(),
                messages.size());
        int nullMessages = 0;
        messages.removeAll(Collections.singleton(null));
        // persist the item
        for (int i = 0; i < messages.size(); i++) {
            Message<String> message = messages.get(i);
            try {
                // TODO if snapshot message discard?

                Acknowledgment acknowledgment = message.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);

                String msg = message.getPayload();

                DataCaptureMessage<PaymentOption> paymentOption =
                        this.objectMapper.readValue(msg, new TypeReference<DataCaptureMessage<PaymentOption>>() {
                        });

                if (paymentOption == null) {
                    nullMessages += 1;
                    continue;
                }

                PaymentOption valuesBefore = paymentOption.getBefore();
                PaymentOption valuesAfter = paymentOption.getAfter();

                log.debug(
                        "PaymentOption ingestion called at {} with payment position id {}",
                        LocalDateTime.now(),
                        (valuesAfter != null ? valuesAfter : valuesBefore).getId());

                // Check payment position status
                // TODO gestisci null values
                if (verifyPaymentPositionStatus(valuesBefore, valuesAfter)) continue;

                // Debtor Tax Code Validation
                if (valuesAfter == null || isInvalidFiscalCode(valuesAfter.getFiscalCode())) continue;

                // Check flag opt-in
                // TODO filtro su ec_tax_code recuperando dalla cache Cache Redis il flag OPT-IN
                // se Flag rtp_cache_created_at è null o troppo vecchio (+2 days) chiama l’api RTP per aggiornare la cache (vedi paragrafo su update cache)

                // Retrieve Transfer's data
                // TODO loop: query su DB replica di payment_option per recuperare la last_updated_date e verificare che il DB replica sia allineato
                acknowledgment.nack(Duration.ofSeconds(1)); // TODO nack when db not aligned, duration in config
                // TODO query transfer
                List<Transfer> transferList = List.of(new Transfer());

                // Filter based on Transfer's taxonomy
                if (verifyTransferCategories(transferList)) continue;

                // Anonymize remittance information
                // TODO presidio anonymizer
                String remittanceInformation = transferList.get(0).getRemittanceInformation();


                // Map RTP message
                boolean response = sendMessageRTP(paymentOption, valuesAfter, remittanceInformation);

                if (!response) {
                    throw new RuntimeException(); // TODO create custom exception;
                }

                log.debug("{} RTPMessage sent to eventhub at {}", LOG_PREFIX, LocalDateTime.now());
                acknowledgment.acknowledge(i); // TODO verify ack index if logic of message retry works with nack

            } catch (JsonProcessingException e) {
                handleException(String.format("%s PaymentOption ingestion error JsonProcessingException at %s", LOG_PREFIX, LocalDateTime.now()), e, message);
            } catch (RuntimeException e) { // TODO custom exception
                handleException(String.format("%s PaymentOption ingestion error CUSTOM exception at %s", LOG_PREFIX, LocalDateTime.now()), e, message);
            } catch (Exception e) {
                handleException(String.format("%s PaymentOption ingestion error Generic exception at %s", LOG_PREFIX, LocalDateTime.now()), e, message);
            }
        }

        log.debug(
                "PaymentOption ingested at {}: total messages {} and {} null",
                LocalDateTime.now(),
                messages.size(),
                nullMessages);
    }

    private boolean verifyTransferCategories(List<Transfer> transferList) {
        // TODO all transfers must match?
        if (!transferList.stream().allMatch(transfer -> this.validTransferCategories.contains(transfer.getCategory()))) {
            return true;
        }
        return false;
    }

    private boolean sendMessageRTP(DataCaptureMessage<PaymentOption> paymentOption, PaymentOption valuesAfter, String remittanceInformation) {
        RTPMessage rtpMessage = RTPMessage.builder()
                .id(valuesAfter.getId())
                .operation(paymentOption.getOp()) // TODO is enum necessary?
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
                .build();

        return this.rtpMessageProducer.sendRTPMessage(rtpMessage);
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
                            "^[A-Z]{6}[0-9LMNPQRSTUV]{2}[ABCDEHLMPRST][0-9LMNPQRSTUV]{2}[A-Z][0-9LMNPQRSTUV]{3}[A-Z]$");
            Pattern patternPIVA = Pattern.compile("/^[0-9]{11}$/");

            return !(patternCF.matcher(fiscalCode).find() || patternPIVA.matcher(fiscalCode).find());
        }

        return true;
    }

    private void handleException(String errorMsg, Exception e, Message<String> message) throws Exception {
        log.error(errorMsg);

        Integer numberOfRetries = message.getHeaders().get(KafkaHeaders.DELIVERY_ATTEMPT, Integer.class);
        if (numberOfRetries != null && numberOfRetries < maxRetries) {
            // TODO send to dead letter storage
        } else {
            throw e;
        }
    }
}
