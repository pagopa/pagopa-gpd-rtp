package it.gov.pagopa.gpd.rtp;

import com.azure.storage.blob.BlobServiceClient;
import com.microsoft.applicationinsights.TelemetryClient;
import it.gov.pagopa.gpd.rtp.events.broadcast.RedisPublisher;
import it.gov.pagopa.gpd.rtp.events.broadcast.RedisSubscriber;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.repository.TransferRepository;
import it.gov.pagopa.gpd.rtp.service.OptinService;
import it.gov.pagopa.gpd.rtp.service.impl.IngestionServiceImpl;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MockConfig {
    @MockBean private RedisPublisher redisPublisher;
    @MockBean private RedisSubscriber redisSubscriber;
    @MockBean private KafkaConsumerService kafkaConsumerService;
    @MockBean private OptinService optinService;
    @MockBean private PaymentOptionRepository paymentOptionRepository;
    @MockBean private TransferRepository transferRepository;
    @MockBean private IngestionServiceImpl ingestionServiceImpl;
    @MockBean private BlobServiceClient blobServiceClient;
    @MockBean private TelemetryClient telemetryClient;
}
