package it.gov.pagopa.gpd.rtp.service.impl;

import it.gov.pagopa.gpd.rtp.events.consumer.ProcessingTracker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.stream.binder.Binding;
import org.springframework.cloud.stream.binding.BindingsLifecycleController;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class KafkaConsumerService {

  private final BindingsLifecycleController bindingsLifecycleController;
  private final ProcessingTracker processingTracker;
  public static final String BINDING_NAME = "bindingName";

  public void stopAllConsumers() {
    for (Map<?, ?> elem : bindingsLifecycleController.queryStates()) {
      String bindingName = (String) elem.get(BINDING_NAME);
      bindingsLifecycleController.stop(bindingName);
    }
  }

  public void startAllConsumers() {
    for (Map<?, ?> elem : bindingsLifecycleController.queryStates()) {
      String bindingName = (String) elem.get(BINDING_NAME);
      bindingsLifecycleController.start(bindingName);
    }
  }

  public List<Map<?, ?>> getConsumersDetails() {
    return bindingsLifecycleController.queryStates();
  }

  public Map<String, Object> getStatus() {
    boolean acc = true;
    Map<String, String> details = new HashMap<>();
    for (Map<?, ?> elem : bindingsLifecycleController.queryStates()) {
      String bindingName = (String) elem.get(BINDING_NAME);
      List<Binding<?>> binding = bindingsLifecycleController.queryState(bindingName);
      for (Binding<?> b : binding) {
        details.put(b.getName(), b.isRunning() ? "RUNNING" : "STOPPED");
        boolean running = b.isRunning();
        acc = acc && running;
      }
    }
    boolean consumersRunning = acc;
    boolean noMessagesInFlight = !processingTracker.isProcessing();

    // La condizione di "veramente pronto per lo shutdown"
    boolean readyForShutdown = !consumersRunning && noMessagesInFlight;

    Map<String, Object> response = new HashMap<>();
    response.put("readyForShutdown", readyForShutdown); // Questo è il nostro nuovo flag principale
    response.put("consumersRunning", consumersRunning);
    response.put("activeProcessingCount", processingTracker.getActiveProcessingCount());

    response.put("details", details);

    return response;
  }
}
