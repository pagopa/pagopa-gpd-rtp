package it.gov.pagopa.gpd.rtp.repository;

import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.AppException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCacheRepository {

  private final StringRedisTemplate redisTemplate;
  private static final String KEY = "rtp_flag_optin";
  private static final String CREATED_AT_KEY = "rtp_created_at";
  private static final Duration TTL = Duration.ofDays(7);

  public void saveAll(Collection<String> ids) {
    redisTemplate.opsForSet().add(KEY, ids.toArray(new String[0]));
    redisTemplate.opsForValue().set(CREATED_AT_KEY, LocalDateTime.now().toString());
    redisTemplate.expire(KEY, TTL);
    redisTemplate.expire(CREATED_AT_KEY, TTL);
  }

  public boolean isPresent(String idDominio) {
    String createdAt = redisTemplate.opsForValue().get(CREATED_AT_KEY);
    if (createdAt == null
        || LocalDateTime.now().isAfter(LocalDateTime.parse(createdAt).plusDays(2))) {
      throw new AppException(AppError.REDIS_CACHE_NOT_UPDATED);
    }
    return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(KEY, idDominio));
  }
}
