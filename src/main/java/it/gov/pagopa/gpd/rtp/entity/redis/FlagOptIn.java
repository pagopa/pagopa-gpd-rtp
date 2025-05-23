package it.gov.pagopa.gpd.rtp.entity.redis;

import lombok.Getter;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

import java.io.Serializable;

@Getter
@Setter
@RedisHash("FlagOptIn")
public class FlagOptIn implements Serializable {

    private String id; // Organization Fiscal Code
    private boolean flagOptIn;
}
