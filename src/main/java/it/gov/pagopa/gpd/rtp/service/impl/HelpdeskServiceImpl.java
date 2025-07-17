package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.events.model.DeadLetterMessage;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class HelpdeskServiceImpl implements HelpdeskService {

    private final BlobStorageClient blobStorageClient;
    private final IngestionService ingestionService;
    private final ObjectMapper objectMapper;

    @Override
    public List<String> getBlobList(String year, String month, String day, String hour) {
        return blobStorageClient.getBlobList(year, month, day, hour);
    }

    @Override
    public String getJSONFromBlobStorage(String fileName) {
        return new String(blobStorageClient.getJSONFromBlobStorage(fileName));
    }

    @Override
    public void retryMessage(String fileName) throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage = objectMapper.readValue(new String(blobStorageClient.getJSONFromBlobStorage(fileName)), DeadLetterMessage.class);

        Map<String, Object> headers = Map.of("id", deadLetterMessage.getId(), KafkaHeaders.RECEIVED_KEY, deadLetterMessage.getId());
        Message<String> genericMessage = new GenericMessage<>(objectMapper.writeValueAsString(deadLetterMessage.getOriginalMessage()), headers);
        ingestionService.handleMessage(genericMessage);

        blobStorageClient.deleteBlob(fileName);
    }
}
