package it.gov.pagopa.gpd.rtp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.rtp.service.OptinService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping(path = "/opt-in")
@Tag(name = "Utils", description = "Utility endpoints")
public class OptInController {

  @Autowired OptinService optinService;

  @Operation(
      summary = "Refresh the OPT-IN flags",
      description = "Return OK if Redis Cache is refreshed with opt-in flags",
      security = {@SecurityRequirement(name = "ApiKey")},
      tags = {"Utils"})
  @GetMapping(value = "/refresh")
  @ResponseStatus(HttpStatus.OK)
  public void optInRefresh() {
    optinService.optInRefresh();
  }
}
