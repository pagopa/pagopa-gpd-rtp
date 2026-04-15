package it.gov.pagopa.gpd.rtp.model.helpdesk;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class RetryDeadLetterInfo {
    private List<String> fileNames;
    private int count;

    public RetryDeadLetterInfo(List<String> fileNames) {
        this.fileNames = fileNames;
        this.count = fileNames.size();
    }
}
