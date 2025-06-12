package it.gov.pagopa.gpd.rtp;

import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Component;

@Component
public class ProcessingTracker {

  private final AtomicInteger activeProcessingCount = new AtomicInteger(0);

  /** Da chiamare all'inizio dell'elaborazione di un messaggio. */
  public void messageProcessingStarted() {
    activeProcessingCount.incrementAndGet();
  }

  /**
   * Da chiamare alla fine dell'elaborazione di un messaggio, in un blocco finally per garantire
   * l'esecuzione.
   */
  public void messageProcessingFinished() {
    activeProcessingCount.decrementAndGet();
  }

  /**
   * Controlla se ci sono elaborazioni in corso.
   *
   * @return true se il contatore è maggiore di zero.
   */
  public boolean isProcessing() {
    return activeProcessingCount.get() > 0;
  }

  /** Restituisce il numero di messaggi attualmente in elaborazione. */
  public int getActiveProcessingCount() {
    return activeProcessingCount.get();
  }
}
