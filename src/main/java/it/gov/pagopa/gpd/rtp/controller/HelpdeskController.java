package it.gov.pagopa.gpd.rtp.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/error-messages")
@RequiredArgsConstructor
@Tag(name = "API Helpdesk", description = "APIs to retry failed RTP messages")
public class HelpdeskController {

    private final HelpdeskService helpdeskService;

    @GetMapping(value = "", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getBlobList(
            @RequestParam(value = "year", required = false) String year,
            @RequestParam(value = "month", required = false) String month,
            @RequestParam(value = "day", required = false) String day,
            @RequestParam(value = "hour", required = false) String hour
    ) {
        return ResponseEntity.ok(helpdeskService.getBlobList(year, month, day, hour));
    }

    @GetMapping(value = "/detail", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getJSONFromBlobStorage(@RequestParam(value = "filename") String filename) {
        return ResponseEntity.ok(helpdeskService.getJSONFromBlobStorage(filename));
    }

    @PostMapping(value = "/retry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> retryMessage(@RequestParam(value = "filename") String filename) throws JsonProcessingException {
        helpdeskService.retryMessage(filename);
        return ResponseEntity.ok("Message retried");
    }
}
