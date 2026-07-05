package com.quanlycuahang.erp.auth.security;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Luu tokenFamily -> jti hop le hien tai trong Redis (D3 rotation + reuse detection). Moi
 * tokenFamily chi co dung 1 jti hop le tai 1 thoi diem — refresh bang jti khac (da bi rotate qua)
 * nghia la token cu bi danh cap hoac dung lai, phai thu hoi ca chuoi.
 */
@Service
public class RefreshTokenService {

  private static final String KEY_PREFIX = "refresh:family:";

  private final StringRedisTemplate redisTemplate;

  public RefreshTokenService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void storeCurrentJti(String tokenFamily, String jti, long ttlMillis) {
    redisTemplate.opsForValue().set(KEY_PREFIX + tokenFamily, jti, Duration.ofMillis(ttlMillis));
  }

  public Optional<String> getCurrentJti(String tokenFamily) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + tokenFamily));
  }

  public void revokeFamily(String tokenFamily) {
    redisTemplate.delete(KEY_PREFIX + tokenFamily);
  }
}
