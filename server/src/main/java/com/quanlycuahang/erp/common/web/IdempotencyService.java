package com.quanlycuahang.erp.common.web;

import java.time.Duration;
import java.util.Optional;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * Chong tao trung don khi double-click/mang chap chon retry (B4 edge case 7): FE gui header
 * Idempotency-Key (UUID), Backend "reserve" khoa trong Redis truoc khi xu ly, luu ket qua (orderId)
 * sau khi thanh cong. Request lap lai voi cung key tra ve dung ket qua lan dau, khong tao don thu
 * 2.
 */
@Service
public class IdempotencyService {

  private static final String KEY_PREFIX = "idempotency:";
  private static final String PROCESSING_MARKER = "PROCESSING";
  private static final Duration TTL = Duration.ofHours(24);

  private final StringRedisTemplate redisTemplate;

  public IdempotencyService(StringRedisTemplate redisTemplate) {
    this.redisTemplate = redisTemplate;
  }

  public Optional<String> getCompletedResult(String idempotencyKey) {
    String value = redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey);
    if (value == null || PROCESSING_MARKER.equals(value)) {
      return Optional.empty();
    }
    return Optional.of(value);
  }

  public boolean isProcessing(String idempotencyKey) {
    return PROCESSING_MARKER.equals(redisTemplate.opsForValue().get(KEY_PREFIX + idempotencyKey));
  }

  /**
   * @return true neu claim thanh cong (chua ai xu ly key nay), false neu da co request khac dang xu
   *     ly/da xong.
   */
  public boolean tryClaim(String idempotencyKey) {
    Boolean claimed =
        redisTemplate
            .opsForValue()
            .setIfAbsent(KEY_PREFIX + idempotencyKey, PROCESSING_MARKER, TTL);
    return Boolean.TRUE.equals(claimed);
  }

  public void complete(String idempotencyKey, String resultValue) {
    redisTemplate.opsForValue().set(KEY_PREFIX + idempotencyKey, resultValue, TTL);
  }

  /** Goi khi xu ly that bai de client co the thu lai voi cung key. */
  public void release(String idempotencyKey) {
    redisTemplate.delete(KEY_PREFIX + idempotencyKey);
  }
}
