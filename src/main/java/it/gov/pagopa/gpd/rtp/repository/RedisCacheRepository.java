package it.gov.pagopa.gpd.rtp.repository;

import it.gov.pagopa.gpd.rtp.exception.AppError;
import it.gov.pagopa.gpd.rtp.exception.FailAndNotify;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.SessionCallback;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RedisCacheRepository {

  public static final String SUFFIX_KEY = "rtp_";
  public static final String KEY = "rtp_flag_optin";
  private static final String CREATED_AT_KEY = "rtp_created_at";
  private static final Duration TTL = Duration.ofDays(7);
  private final StringRedisTemplate redisTemplate;

  public void saveAll(Collection<String> ids) {
    // Redis transaction to prevent delete if save goes wrong
    redisTemplate.execute(new SessionCallback<List<Object>>() {
      @Override
      public List<Object> execute(@NotNull RedisOperations operations) {
        try {
          operations.multi();

          operations.delete(KEY);
          operations.opsForSet().add(KEY, ids.toArray(String[]::new));
          operations.opsForValue().set(CREATED_AT_KEY, LocalDateTime.now().toString());
          operations.expire(KEY, TTL);
          operations.expire(CREATED_AT_KEY, TTL);

          return operations.exec();
        } catch (RuntimeException e) {
          operations.discard();
          throw e;
        }
      }
    });
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
