package it.gov.pagopa.gpd.rtp.events.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import it.gov.pagopa.gpd.rtp.events.model.enumeration.DebeziumOperationCode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class DataCaptureMessage<T> {

    private T before;
    private T after;
    private DebeziumOperationCode op;
    @JsonProperty("ts_ms")
    private Long tsMs;
    @JsonProperty("ts_us")
    private Long tsUs;
    @JsonProperty("ts_ns")
    private Long tsNs;
}
