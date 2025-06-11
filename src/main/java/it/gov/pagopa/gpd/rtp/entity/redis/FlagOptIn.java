package it.gov.pagopa.gpd.rtp.entity.redis;

import java.io.Serializable;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Getter
@Setter
@RedisHash("FlagOptIn")
@Builder
public class FlagOptIn implements Serializable {

  @Id private String idEc; // Organization Fiscal Code
  private boolean flagValue;
}
