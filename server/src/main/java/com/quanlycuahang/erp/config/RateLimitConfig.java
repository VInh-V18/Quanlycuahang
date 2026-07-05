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

  @Bean(destroyMethod = "shutdown")
  public RedisClient rateLimitRedisClient(
      @Value("${spring.data.redis.host}") String host,
      @Value("${spring.data.redis.port}") int port) {
    return RedisClient.create(RedisURI.Builder.redis(host, port).build());
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
