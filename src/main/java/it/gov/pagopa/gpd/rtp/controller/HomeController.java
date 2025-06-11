package it.gov.pagopa.gpd.rtp.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import it.gov.pagopa.gpd.rtp.model.AppInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;

@RestController
@Validated
public class HomeController {

  @Value("${info.application.name}")
  private String name;

  @Value("${info.application.version}")
  private String version;

  @Value("${info.properties.environment}")
  private String environment;

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
}
