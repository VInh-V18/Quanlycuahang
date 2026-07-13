package com.quanlycuahang.erp.config;

import io.github.bucket4j.distributed.ExpirationAfterWriteStrategy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.redis.lettuce.cas.LettuceBasedProxyManager;
import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ha tang rate limit Bucket4j + Redis (D3: dang nhap 5 lan/15 phut/IP, API chung 100
 * req/phut/user).
 */
@Configuration
public class RateLimitConfig {

  /**
   * Client Redis RIENG cho Bucket4j (thu vien nay can truc tiep RedisClient cua Lettuce, khong dung
   * lai duoc StringRedisTemplate/LettuceConnectionFactory Spring Boot da tu cau hinh) - truoc day
   * chi doc host/port, BO QUA spring.data.redis.password: ngay khi dat REDIS_PASSWORD that o
   * production, client nay se khong xac thuc duoc, am tham lam giam chan brute-force dang nhap va
   * gioi han 100 req/phut ngung hoat dong (phat hien khi rieng soat bao mat). Chi goi withPassword
   * khi co cau hinh (rong = moi truong dev khong bat auth, giu nguyen hanh vi cu, tranh gui AUTH
   * toi Redis khong yeu cau xac thuc).
   */
  @Bean(destroyMethod = "shutdown")
  public RedisClient rateLimitRedisClient(
      @Value("${spring.data.redis.host}") String host,
      @Value("${spring.data.redis.port}") int port,
      @Value("${spring.data.redis.password:}") String password) {
    RedisURI.Builder uriBuilder = RedisURI.Builder.redis(host, port);
    if (password != null && !password.isBlank()) {
      uriBuilder.withPassword((CharSequence) password);
    }
    return RedisClient.create(uriBuilder.build());
  }

  @Bean
  public ProxyManager<byte[]> bucketProxyManager(RedisClient redisClient) {
    return LettuceBasedProxyManager.builderFor(redisClient)
        .withExpirationStrategy(
            ExpirationAfterWriteStrategy.basedOnTimeForRefillingBucketUpToMax(
                Duration.ofMinutes(30)))
        .build();
  }
}
