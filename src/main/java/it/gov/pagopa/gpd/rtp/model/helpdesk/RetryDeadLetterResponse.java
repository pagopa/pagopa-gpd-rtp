package it.gov.pagopa.gpd.rtp.model.helpdesk;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Getter
@NoArgsConstructor
public class RetryDeadLetterResponse {
    @Schema(description = "Messages successfully retried")
    private RetryDeadLetterInfo retrySuccessful;
    @Schema(description = "Messages ignored because outdated or not processable")
    private RetryDeadLetterInfo retryIgnored;
    @Schema(description = "Messages ignored because newer than the minutes offset defined in the request (default 2)")
    private RetryDeadLetterInfo retryPostponed;
    @Schema(description = "Messages failed to be sent to eventhub, need a retry")
    private RetryDeadLetterInfo retryFailed;
    private int totalCount;

    public RetryDeadLetterResponse(int totalCount, Map<RetryDeadLetterEnum, List<String>> retryOutcomes){
        this.retrySuccessful = new RetryDeadLetterInfo(retryOutcomes.get(RetryDeadLetterEnum.RETRY_SUCCESSFUL));
        this.retryIgnored = new RetryDeadLetterInfo(retryOutcomes.get(RetryDeadLetterEnum.RETRY_IGNORED));
        this.retryPostponed = new RetryDeadLetterInfo(retryOutcomes.get(RetryDeadLetterEnum.RETRY_POSTPONED));
        this.retryFailed = new RetryDeadLetterInfo(retryOutcomes.get(RetryDeadLetterEnum.RETRY_FAILED));
        this.totalCount = totalCount;
    }
}


