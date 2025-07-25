package it.gov.pagopa.gpd.rtp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.rtp.model.ProblemJson;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/error-messages")
@RequiredArgsConstructor
@Tag(name = "API Helpdesk", description = "APIs to retry failed RTP messages")
public class HelpdeskController {

    private final HelpdeskService helpdeskService;

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Service unavailable", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class)))
    })
    @Operation(summary = "Get list of failed messages saved on blob storage as JSON files", security = {@SecurityRequirement(name = "JWT")})
    public List<String> getBlobList(
            @Parameter(description = "Filter by year", example = "2025") @RequestParam(value = "year", required = false) String year,
            @Parameter(description = "Filter by month, requires year otherwise gets ignored", example = "5") @RequestParam(value = "month", required = false) String month,
            @Parameter(description = "Filter by day, requires month otherwise gets ignored", example = "7") @RequestParam(value = "day", required = false) String day,
            @Parameter(description = "Filter by hours, requires day otherwise gets ignored", example = "1") @RequestParam(value = "hour", required = false) String hour
    ) {
        return helpdeskService.getBlobList(year, month, day, hour);
    }

    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Service unavailable", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class)))
    })
    @Operation(summary = "Retrieve the specified error message JSON by its filename", security = {@SecurityRequirement(name = "JWT")})
    public String getJSONFromBlobStorage(
            @Parameter(description = "Filename of the failed message's JSON to retrieve") @RequestParam(value = "filename") String filename) {
        return helpdeskService.getJSONFromBlobStorage(filename);
    }

    @PostMapping(value = "/retry", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "OK", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema())),
            @ApiResponse(responseCode = "400", description = "Bad Request", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "401", description = "Unauthorized", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "404", description = "Not found", content = @Content(schema = @Schema(implementation = ProblemJson.class))),
            @ApiResponse(responseCode = "429", description = "Too many requests", content = @Content(schema = @Schema())),
            @ApiResponse(responseCode = "500", description = "Service unavailable", content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = ProblemJson.class)))
    })
    @Operation(summary = "Retry the specified error message JSON by its filename", security = {@SecurityRequirement(name = "JWT")})
    public String retryMessage(
            @Parameter(description = "Filename of the failed message's JSON to retry") @RequestParam(value = "filename") String filename) throws JsonProcessingException {
        return helpdeskService.retryMessage(filename);
    }
}
