package it.gov.pagopa.gpd.rtp;

import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import it.gov.pagopa.gpd.rtp.service.impl.KafkaConsumerService;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@Setter
public class GracefulShutdownHandler implements SmartLifecycle {

  public final AtomicBoolean forceKill = new AtomicBoolean(false);
  private final KafkaConsumerService kafkaConsumerService;
  private final ProcessingTracker processingTracker;
  private final AtomicBoolean running = new AtomicBoolean(false);

  @Override
  public void start() {
    log.info("GracefulShutdownHandler started.");
    this.running.set(true);
  }

  @Override
  public void stop() {
    log.info("Graceful shutdown initiated by SmartLifecycle.stop().");
    this.running.set(false);

    kafkaConsumerService.stopAllConsumers();
    log.info("Kafka consumers stopped. Waiting for in-flight messages to complete...");
    log.info("force kill: {}", forceKill.get());

    if (!forceKill.get()) {

      while (processingTracker.isProcessing()) {
        try {
          log.info(
              "Waiting for {} messages to finish processing...",
              processingTracker.getActiveProcessingCount());
          Thread.sleep(500);
        } catch (InterruptedException e) {
          log.warn("Shutdown wait was interrupted.");
          Thread.currentThread().interrupt();
          break;
        }
      }
    }

    log.info("All in-flight messages processed. Graceful shutdown completed.");
  }

  @Override
  public boolean isRunning() {
    return this.running.get();
  }

  @Override
  public int getPhase() {
    return Integer.MAX_VALUE - 1000;
  }

  public void withForceKill(boolean b) {
    this.forceKill.set(b);
  }
}
