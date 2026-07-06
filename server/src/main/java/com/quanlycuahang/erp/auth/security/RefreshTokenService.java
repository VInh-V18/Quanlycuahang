package com.quanlycuahang.erp.auth.security;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Luu tokenFamily -> jti hop le hien tai trong Redis (D3 rotation + reuse detection). Moi
 * tokenFamily chi co dung 1 jti hop le tai 1 thoi diem — refresh bang jti khac (da bi rotate qua VA
 * da het han grace, xem duoi) nghia la token cu bi danh cap hoac dung lai, phai thu hoi ca chuoi.
 *
 * <p>Moi jti vua bi rotate qua duoc ghi lai rieng le (khoa "retired:{jti}", TTL = GRACE_PERIOD)
 * thay vi 1 o nho duy nhat co the bi ghi de — vi thuc te co the co NHIEU hon 2 request refresh gan
 * nhu dong thoi cung dung 1 token con hop le (React StrictMode goi bootstrapSession 2 lan luc dev,
 * cong them 1 request khac dang cho 401 tu truoc do cung kich hoat refresh) — neu chi giu 1 jti
 * "grace" duy nhat, rotation thu 3 se ghi de mat dau vet cua jti dau tien va van bi bao nham la
 * reuse. Dung 1 khoa rieng/jti tranh hoan toan gioi han so luong nguoi tham gia cuoc dua.
 */
@Service
public class RefreshTokenService {

  private static final String KEY_PREFIX = "refresh:family:";
  private static final String RETIRED_PREFIX = "refresh:family:retired:";
  private static final Duration GRACE_PERIOD = Duration.ofSeconds(30);

  private final StringRedisTemplate redisTemplate;

  public RefreshTokenService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void storeCurrentJti(String tokenFamily, String jti, long ttlMillis) {
    String previousJti = redisTemplate.opsForValue().get(KEY_PREFIX + tokenFamily);
    if (previousJti != null) {
      redisTemplate.opsForValue().set(retiredKey(tokenFamily, previousJti), "1", GRACE_PERIOD);
    }
    redisTemplate.opsForValue().set(KEY_PREFIX + tokenFamily, jti, Duration.ofMillis(ttlMillis));
  }

  public Optional<String> getCurrentJti(String tokenFamily) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + tokenFamily));
  }

  public boolean isWithinGracePeriod(String tokenFamily, String jti) {
    return Boolean.TRUE.equals(redisTemplate.hasKey(retiredKey(tokenFamily, jti)));
  }

  public void revokeFamily(String tokenFamily) {
    redisTemplate.delete(KEY_PREFIX + tokenFamily);
  }

  private static String retiredKey(String tokenFamily, String jti) {
    return RETIRED_PREFIX + tokenFamily + ":" + jti;
  }
}
