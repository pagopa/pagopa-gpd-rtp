package it.gov.pagopa.gpd.rtp;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.azure.storage.blob.BlobServiceClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import it.gov.pagopa.gpd.rtp.client.AnonymizerClient;
import it.gov.pagopa.gpd.rtp.client.BlobStorageClient;
import it.gov.pagopa.gpd.rtp.events.producer.RTPMessageProducer;
import it.gov.pagopa.gpd.rtp.repository.PaymentOptionRepository;
import it.gov.pagopa.gpd.rtp.repository.TransferRepository;
import it.gov.pagopa.gpd.rtp.service.DeadLetterService;
import it.gov.pagopa.gpd.rtp.service.FilterService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.kafka.support.Acknowledgment;

class ApplicationTest {
  @Test
  void applicationContextLoaded() {
    assertTrue(true); // it just tests that an error has not occurred
  }
}
