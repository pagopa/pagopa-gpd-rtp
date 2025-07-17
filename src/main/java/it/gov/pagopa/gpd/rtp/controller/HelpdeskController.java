package it.gov.pagopa.gpd.rtp.controller;

import com.azure.core.annotation.QueryParam;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.gov.pagopa.gpd.rtp.service.HelpdeskService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/helpdesk/blob")
@RequiredArgsConstructor
@Tag(name = "API Helpdesk", description = "APIs to retry failed RTP messages")
public class HelpdeskController {

    private final HelpdeskService helpdeskService;

    @GetMapping(value = "/list", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> getBlobList(
            @QueryParam(value = "year") String year,
            @QueryParam(value = "month") String month,
            @QueryParam(value = "day") String day,
            @QueryParam(value = "hour") String hour
    ) {
        return ResponseEntity.ok(helpdeskService.getBlobList(year, month, day, hour));
    }

    @GetMapping(value = "/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getJSONFromBlobStorage(@QueryParam(value = "fileName") String fileName) {
        return ResponseEntity.ok(helpdeskService.getJSONFromBlobStorage(fileName));
    }

    @PostMapping(value = "/retry", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> retryMessage(@QueryParam(value = "fileName") String fileName) throws JsonProcessingException {
        helpdeskService.retryMessage(fileName);
        return ResponseEntity.ok("Message retried");
    }
}
