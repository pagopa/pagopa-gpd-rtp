package it.gov.pagopa.gpd.rtp.repository;

import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.FailAndNotify;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCacheRepository {

  private final StringRedisTemplate redisTemplate;
  public static final String SUFFIX_KEY = "rtp_";
  public static final String KEY = "rtp_flag_optin";
  private static final String CREATED_AT_KEY = "rtp_created_at";
  private static final Duration TTL = Duration.ofDays(7);

  public void saveAll(Collection<String> ids) {
    redisTemplate.opsForSet().add(KEY, ids.toArray(new String[0]));
    redisTemplate.opsForValue().set(CREATED_AT_KEY, LocalDateTime.now().toString());
    redisTemplate.expire(KEY, TTL);
    redisTemplate.expire(CREATED_AT_KEY, TTL);
  }

  public void setRetryCount(String id, int retryCount) {
    redisTemplate.opsForValue().set(SUFFIX_KEY + id, String.valueOf(retryCount));
    redisTemplate.expire(SUFFIX_KEY + id, Duration.ofHours(2));
  }

  public void deleteRetryCount(String id) {
    redisTemplate.delete(SUFFIX_KEY + id);
  }

  public int getRetryCount(String id) {
    return Integer.parseInt(
        Optional.ofNullable(redisTemplate.opsForValue().get(SUFFIX_KEY + id)).orElse("0"));
  }

  @NotNull
  @Cacheable(value = "getFlags")
  public SetOperations<String, String> getFlags() {
    return redisTemplate.opsForSet();
  }

  @Cacheable(value = "isCacheUpdated")
  public boolean isCacheUpdated() {
    String createdAt = redisTemplate.opsForValue().get(CREATED_AT_KEY);
    if (createdAt == null
        || LocalDateTime.now().isAfter(LocalDateTime.parse(createdAt).plusDays(3))) {
      throw new FailAndNotify(AppError.REDIS_CACHE_NOT_UPDATED);
    }
    return true;
  }
}
