package it.gov.pagopa.gpd.rtp.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.rtp.model.ProblemJson;
import it.gov.pagopa.gpd.rtp.model.helpdesk.RetryDeadLetterResponse;
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
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "OK",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, array = @ArraySchema(schema = @Schema(implementation = String.class)))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Too many requests",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Service unavailable",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class)))
            })
    @Operation(summary = "Get list of failed messages saved on blob storage as JSON files")
    public List<String> getBlobList(
            @Parameter(description = "Filter by year", example = "2025")
            @RequestParam(value = "year", required = false)
            String year,
            @Parameter(
                    description = "Filter by month, requires year otherwise gets ignored",
                    example = "5")
            @RequestParam(value = "month", required = false)
            String month,
            @Parameter(
                    description = "Filter by day, requires month otherwise gets ignored",
                    example = "7")
            @RequestParam(value = "day", required = false)
            String day,
            @Parameter(
                    description = "Filter by hours, requires day otherwise gets ignored",
                    example = "1")
            @RequestParam(value = "hour", required = false)
            String hour,
            @Parameter(description = "Number of messages to retry (default 400)", example = "400")
            @RequestParam(value = "maxMessages", required = false, defaultValue = "400")
            int maxMessages
            ) {
        return this.helpdeskService.getBlobList(year, month, day, hour, maxMessages);
    }

    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "OK",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = String.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not found",
                            content = @Content(schema = @Schema(implementation = ProblemJson.class))),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Too many requests",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Service unavailable",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class)))
            })
    @Operation(summary = "Retrieve the specified error message JSON by its filename")
    public String getJSONFromBlobStorage(
            @Parameter(description = "Filename of the failed message's JSON to retrieve")
            @RequestParam(value = "filename")
            String filename) {
        return this.helpdeskService.getJSONFromBlobStorage(filename);
    }

    @PostMapping(value = "/retry", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "OK",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RetryDeadLetterResponse.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not found",
                            content = @Content(schema = @Schema(implementation = ProblemJson.class))),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Too many requests",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Service unavailable",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class)))
            })
    @Operation(summary = "Retry a list of error messages")
    public RetryDeadLetterResponse retryMessages(
            @Parameter(description = "Ignore the messages newer than the defined minutes (default 2)", example = "2")
            @RequestParam(value = "minutesOffset", required = false, defaultValue = "2")
            int minutesOffset,
            @RequestBody List<String> filenames) {
        return this.helpdeskService.retryMessages(filenames, minutesOffset);
    }

    @PostMapping(value = "/retry/all", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.OK)
    @ApiResponses(
            value = {
                    @ApiResponse(
                            responseCode = "200",
                            description = "OK",
                            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE, schema = @Schema(implementation = RetryDeadLetterResponse.class))),
                    @ApiResponse(
                            responseCode = "400",
                            description = "Bad Request",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class))),
                    @ApiResponse(
                            responseCode = "401",
                            description = "Unauthorized",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "404",
                            description = "Not found",
                            content = @Content(schema = @Schema(implementation = ProblemJson.class))),
                    @ApiResponse(
                            responseCode = "429",
                            description = "Too many requests",
                            content = @Content(schema = @Schema())),
                    @ApiResponse(
                            responseCode = "500",
                            description = "Service unavailable",
                            content =
                            @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = ProblemJson.class)))
            })
    @Operation(summary = "Retry all error messages")
    public RetryDeadLetterResponse retryAllMessages(
            @Parameter(description = "Ignore the messages newer than the defined minutes (default 2)", example = "5")
            @RequestParam(value = "minutesOffset", required = false, defaultValue = "2")
            int minutesOffset,
            @Parameter(description = "Number of messages to retry (default 400)", example = "400")
            @RequestParam(value = "maxMessages", required = false, defaultValue = "400")
            int maxMessages
    ) {
        List<String> filenames = this.helpdeskService.getBlobList(null, null, null, null, maxMessages);
        return this.helpdeskService.retryMessages(filenames, minutesOffset);
    }
}
