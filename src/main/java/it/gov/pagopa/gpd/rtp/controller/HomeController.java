package it.gov.pagopa.gpd.rtp.controller;

import io.fabric8.kubernetes.api.model.LabelSelector;
import io.fabric8.kubernetes.api.model.Pod;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.gpd.rtp.model.AppInfo;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
@RequiredArgsConstructor
public class HomeController {

  @Value("${info.application.name}")
  private String name;

  @Value("${info.application.version}")
  private String version;

  @Value("${info.properties.environment}")
  private String environment;

  private final KubernetesClient client;
  private final RestTemplate restTemplate;

  @Hidden
  @GetMapping("")
  public RedirectView home() {
    return new RedirectView("/swagger-ui.html");
  }

  @Operation(
      summary = "health check",
      description = "Return OK if application is started",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {"Home"})
  @GetMapping(value = "/info")
  @ResponseStatus(HttpStatus.OK)
  public ResponseEntity<AppInfo> healthCheck() {
    // Used just for health checking
    AppInfo info = AppInfo.builder().name(name).version(version).environment(environment).build();
    return ResponseEntity.status(HttpStatus.OK).body(info);
  }

  @GetMapping("/are-all-done")
  public ResponseEntity<String> allDone() {
    boolean done = allPodsIdle();
    return ResponseEntity.ok(done ? "ALL_DONE" : "STILL_RUNNING");
  }

  public boolean allPodsIdle() {
    var labelSelectorObj = new LabelSelector();
    Map<String, String> matchLabels = new java.util.HashMap<>();
    matchLabels.put("app.kubernetes.io/instance", "pagopa-gpd-rtp");
    matchLabels.put("app.kubernetes.io/version", version);
    labelSelectorObj.setMatchLabels(matchLabels);
    List<Pod> pods =
        client.pods().inNamespace("gps").withLabelSelector(labelSelectorObj).list().getItems();

    for (Pod pod : pods) {
      String podIP = pod.getStatus().getPodIP();
      String url = "http://" + podIP + ":8080/status";
      try {
        Map<?, ?> response = restTemplate.getForObject(url, Map.class);
        Boolean inProgress = (Boolean) response.get("inProgress");
        if (inProgress != null && inProgress) {
          return false;
        }
      } catch (Exception e) {
        // log errore
        return false;
      }
    }

    return true;
  }
}
