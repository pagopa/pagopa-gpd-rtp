package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.AnonymizerClient;
import it.gov.pagopa.gpd.rtp.config.KafkaConfig;
import it.gov.pagopa.gpd.rtp.entity.PaymentOption;
import it.gov.pagopa.gpd.rtp.entity.Transfer;
import it.gov.pagopa.gpd.rtp.events.model.DataCaptureMessage;
import it.gov.pagopa.gpd.rtp.events.model.PaymentOptionEvent;
import it.gov.pagopa.gpd.rtp.events.model.RTPMessage;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.RTPOperationCode;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.repository.TransferRepository;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class IngestionServiceImpl implements IngestionService {
    private static final String LOG_PREFIX = "[GPDxRTP]";

    // TODO category with 9/.../ or remove from transfer's string?


    private final ObjectMapper objectMapper;
    private final RTPMessageProducer rtpMessageProducer;
    private final FilterService filterService;
    private final TransferRepository transferRepository;
    private final PaymentOptionRepository paymentOptionRepository;
    private final AnonymizerClient anonymizerClient;

    @Autowired
    public IngestionServiceImpl(
            ObjectMapper objectMapper,
            RTPMessageProducer rtpMessageProducer,

            FilterService filterService,
            TransferRepository transferRepository, PaymentOptionRepository paymentOptionRepository, AnonymizerClient anonymizerClient) {
        this.objectMapper = objectMapper;
        this.rtpMessageProducer = rtpMessageProducer;
        this.filterService = filterService;
        this.transferRepository = transferRepository;
        this.paymentOptionRepository = paymentOptionRepository;
        this.anonymizerClient = anonymizerClient;
    }

    public void ingestPaymentOptions(Message<List<String>> messages) {
        List<String> messageList = messages.getPayload();

        Acknowledgment acknowledgment = messages.getHeaders().get(KafkaHeaders.ACKNOWLEDGMENT, Acknowledgment.class);

        log.debug(
                "PaymentOption ingestion called at {} for payment options with events list size {}",
                LocalDateTime.now(),
                messageList.size());

        messageList.removeAll(Collections.singleton(null));

        // persist the item
        for (int i = 0; i < messageList.size(); i++) {
            String msg = messageList.get(i);

            try {
                DataCaptureMessage<PaymentOptionEvent> paymentOption =
                        this.objectMapper.readValue(msg, new TypeReference<DataCaptureMessage<PaymentOptionEvent>>() {
                        });

                RTPMessage rtpMessage = elaborateRTPMessage(paymentOption);

                boolean response = this.rtpMessageProducer.sendRTPMessage(rtpMessage);
                if (!response) {
                    throw new AppException(AppError.RTP_MESSAGE_NOT_SENT);
                }
                log.debug("{} RTPMessage sent to eventhub at {}", LOG_PREFIX, LocalDateTime.now());
                acknowledgment.acknowledge(i); // TODO verify ack index if logic of message retry works with nack

            } catch (JsonProcessingException e) {
                handleException(String.format("%s PaymentOption ingestion error JsonProcessingException at %s", LOG_PREFIX, LocalDateTime.now()));
            } catch (AppException e) {
                if (e.getAppErrorCode().equals(AppError.RTP_MESSAGE_NOT_SENT)) {
                    handleException(String.format("%s Error sending RTP message to eventhub at %s", LOG_PREFIX, LocalDateTime.now()));
                } else if (e.getAppErrorCode().equals(AppError.DB_REPLICA_NOT_UPDATED)) {
                    acknowledgment.nack(i, Duration.ofSeconds(1)); // TODO avoid loop
                } else {
                    acknowledgment.acknowledge(i); // TODO verify ack index if logic of message retry works with nack
                }
            } catch (Exception e) {
                handleException(String.format("%s PaymentOption ingestion error Generic exception at %s", LOG_PREFIX, LocalDateTime.now()));
            }
        }

        log.debug(
                "PaymentOptions ingested at {}: total messages {}",
                LocalDateTime.now(),
                messageList.size());
    }

    private RTPMessage elaborateRTPMessage(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
        if (paymentOption.getOp().equals(DebeziumOperationCode.d)) {
            // Map RTP delete message
            return mapRTPDeleteMessage(paymentOption);
        } else if (paymentOption.getOp().equals(DebeziumOperationCode.c) || paymentOption.getOp().equals(DebeziumOperationCode.u)) {
            // Filter paymentOption message, throws AppException
            this.filterService.isValidPaymentOptionForRTPOrElseThrow(paymentOption);

            PaymentOptionEvent valuesAfter = paymentOption.getAfter();

            log.debug(
                    "PaymentOption ingestion called at {} with payment option id {}",
                    LocalDateTime.now(),
                    valuesAfter.getId());

            verifyDBReplicaSync(valuesAfter);

            // Retrieve Transfer's data
            List<Transfer> transferList = this.transferRepository.findByPaymentOptionId(valuesAfter.getId());
            // Filter based on Transfer's categories, throws AppException
            this.filterService.hasValidTransferCategoriesOrElseThrow(transferList);
            String remittanceInformation = anonymizeRemittanceInformation(valuesAfter, transferList);

            return mapRTPMessage(paymentOption, remittanceInformation);
        } else {
            throw new AppException(AppError.CDC_OPERATION_NOT_VALID_FOR_RTP);
        }
    }

    private void verifyDBReplicaSync(PaymentOptionEvent valuesAfter) {
        PaymentOption poFromDBReplica = paymentOptionRepository.findById(valuesAfter.getId()).orElseThrow(() -> new AppException(AppError.DB_REPLICA_NOT_UPDATED));
        Instant poMessageInstant = Instant.ofEpochMilli(valuesAfter.getLastUpdatedDate() / 1000);
        LocalDateTime poMessageDate = LocalDateTime.ofInstant(poMessageInstant, ZoneOffset.UTC);
        if (poFromDBReplica == null || poFromDBReplica.getLastUpdatedDate().isBefore(poMessageDate)) {
            throw new AppException(AppError.DB_REPLICA_NOT_UPDATED);
        }
    }

    private String anonymizeRemittanceInformation(PaymentOptionEvent valuesAfter, List<Transfer> transferList) {
        Transfer primaryTransfer = transferList.stream().filter(el -> el.getOrganizationFiscalCode().equals(valuesAfter.getOrganizationFiscalCode())).findFirst().orElseThrow(() -> new AppException(AppError.TRANSFER_NOT_VALID_FOR_RTP));
        return this.anonymizerClient.anonymize(primaryTransfer.getRemittanceInformation());
    }

    private RTPMessage mapRTPMessage(DataCaptureMessage<PaymentOptionEvent> paymentOption, String remittanceInformation) {
        PaymentOptionEvent valuesAfter = paymentOption.getAfter();
        return RTPMessage.builder()
                .id(valuesAfter.getId())
                .operation(paymentOption.getOp().equals(DebeziumOperationCode.c) ? RTPOperationCode.CREATE : RTPOperationCode.UPDATE)
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
    }

    private RTPMessage mapRTPDeleteMessage(DataCaptureMessage<PaymentOptionEvent> paymentOption) {
        return RTPMessage.builder()
                .id(paymentOption.getBefore().getId())
                .operation(RTPOperationCode.DELETE)
                .timestamp(paymentOption.getTsMs())
                .build();
    }

    private void handleException(String errorMsg) {
        log.error(errorMsg);

        new KafkaConfig().kafkaListenerErrorHandler(); // TODO
    }
}
