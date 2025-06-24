package it.gov.pagopa.gpd.rtp.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@SpringBootTest(classes = RedisCacheRepository.class)
class RedisCacheRepositoryTest {

  @MockBean StringRedisTemplate stringRedisTemplate;

  @Autowired @InjectMocks RedisCacheRepository redisCacheRepository;

  @Test
  void saveAll() {
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.add(any(), any())).thenReturn(1L);
    when(stringRedisTemplate.opsForSet()).thenReturn(mock);

    ValueOperations op = Mockito.mock(ValueOperations.class);
    when(stringRedisTemplate.opsForValue()).thenReturn(op);

    redisCacheRepository.saveAll(List.of("1234"));
  }

  @Test
  void getFlags() {
    SetOperations mock = Mockito.mock(SetOperations.class);
    when(mock.isMember(anyString(), anyString())).thenReturn(true);
    when(stringRedisTemplate.opsForSet()).thenReturn(mock);
    redisCacheRepository.getFlags();
    verifyNoMoreInteractions(mock);
  }

  @Test
  void isCacheUpdated() {
    ValueOperations mock = Mockito.mock(ValueOperations.class);
    when(mock.get(anyString())).thenReturn(LocalDateTime.now().toString());
    when(stringRedisTemplate.opsForValue()).thenReturn(mock);
    var res = redisCacheRepository.isCacheUpdated();
    assertTrue(res);
  }

  @Test
  void setRetryCount() {
    ValueOperations mock = Mockito.mock(ValueOperations.class);
    when(stringRedisTemplate.opsForValue()).thenReturn(mock);
    redisCacheRepository.setRetryCount(UUID.randomUUID(), 1);
  }

  @Test
  void getRetryCount() {
    ValueOperations mock = Mockito.mock(ValueOperations.class);
    when(stringRedisTemplate.opsForValue()).thenReturn(mock);
    var retry = redisCacheRepository.getRetryCount(UUID.randomUUID());
    assertEquals(0, retry);
  }

  @Test
  void deleteRetryCount() {
    ValueOperations mock = Mockito.mock(ValueOperations.class);
    when(stringRedisTemplate.opsForValue()).thenReturn(mock);
    redisCacheRepository.deleteRetryCount(UUID.randomUUID());
  }
}
