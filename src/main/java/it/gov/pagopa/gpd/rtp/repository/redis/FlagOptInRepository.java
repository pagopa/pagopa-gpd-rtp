package it.gov.pagopa.gpd.rtp.repository.redis;

import it.gov.pagopa.gpd.rtp.entity.redis.FlagOptIn;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FlagOptInRepository extends CrudRepository<FlagOptIn, String> {}