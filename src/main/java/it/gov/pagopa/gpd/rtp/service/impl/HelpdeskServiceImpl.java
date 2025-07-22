package it.gov.pagopa.gpd.rtp.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.events.model.DeadLetterMessage;
import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import it.gov.pagopa.gpd.rtp.service.IngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public String retryMessage(String fileName) throws JsonProcessingException {
        DeadLetterMessage deadLetterMessage = this.objectMapper.readValue(new String(blobStorageClient.getJSONFromBlobStorage(fileName)), DeadLetterMessage.class);
        boolean response = this.ingestionService.retryDeadLetterMessage(deadLetterMessage.getOriginalMessage());

        if (response) {
            this.blobStorageClient.deleteBlob(fileName);
            return "Retry successful, message sent to RTP eventhub";
        }

        throw new AppException(AppError.RTP_MESSAGE_NOT_SENT);
    }
}
