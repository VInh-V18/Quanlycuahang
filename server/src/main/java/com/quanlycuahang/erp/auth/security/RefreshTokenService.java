package com.quanlycuahang.erp.auth.security;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Luu tokenFamily -> jti hop le hien tai trong Redis (D3 rotation + reuse detection). Moi
 * tokenFamily chi co dung 1 jti hop le tai 1 thoi diem — refresh bang jti khac (da bi rotate qua)
 * nghia la token cu bi danh cap hoac dung lai, phai thu hoi ca chuoi.
 *
 * <p>jti vua bi rotate qua duoc giu them trong 1 khoang GRACE_PERIOD ngan (khong thu hoi ngay) —
 * 2 request refresh gan nhu dong thoi voi CUNG 1 refresh token (vd mo 2 tab, F5 lien tuc, hoac 1
 * request nen tang dang xu ly dung luc trang moi tai lai va tu goi bootstrapSession) van la tinh
 * huong hop phap, khong phai bi danh cap — neu thu hoi ca chuoi ngay se dang xuat oan nguoi dung
 * that su chi vi 1 su co dong bo vo hai.
 */
@Service
public class RefreshTokenService {

  private static final String KEY_PREFIX = "refresh:family:";
  private static final String GRACE_PREFIX = "refresh:family:grace:";
  private static final Duration GRACE_PERIOD = Duration.ofSeconds(30);

  private final StringRedisTemplate redisTemplate;

  public RefreshTokenService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public void storeCurrentJti(String tokenFamily, String jti, long ttlMillis) {
    String previousJti = redisTemplate.opsForValue().get(KEY_PREFIX + tokenFamily);
    if (previousJti != null) {
      redisTemplate.opsForValue().set(GRACE_PREFIX + tokenFamily, previousJti, GRACE_PERIOD);
    }
    redisTemplate.opsForValue().set(KEY_PREFIX + tokenFamily, jti, Duration.ofMillis(ttlMillis));
  }

  public Optional<String> getCurrentJti(String tokenFamily) {
    return Optional.ofNullable(redisTemplate.opsForValue().get(KEY_PREFIX + tokenFamily));
  }

  public boolean isWithinGracePeriod(String tokenFamily, String jti) {
    return jti.equals(redisTemplate.opsForValue().get(GRACE_PREFIX + tokenFamily));
  }

  public void revokeFamily(String tokenFamily) {
    redisTemplate.delete(KEY_PREFIX + tokenFamily);
    redisTemplate.delete(GRACE_PREFIX + tokenFamily);
  }
}
