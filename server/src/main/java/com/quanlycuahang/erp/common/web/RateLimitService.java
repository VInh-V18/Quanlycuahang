package com.quanlycuahang.erp.common.web;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.ConsumptionProbe;
import io.github.bucket4j.distributed.BucketProxy;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.stereotype.Service;

/** Rate limit dung chung (Bucket4j + Redis, D3) — dung cho dang nhap va API chung. */
@Service
public class RateLimitService {

  private final ProxyManager<byte[]> proxyManager;

  public RateLimitService(ProxyManager<byte[]> proxyManager) {
    this.proxyManager = proxyManager;
  }

  /**
   * Tra ve true neu request duoc phep (con luot trong bucket), false neu vuot gioi han.
   *
   * @param key khoa dinh danh (vd "login:" + ip, "api:" + userId)
   * @param capacity so luot toi da trong 1 khoang thoi gian
   * @param period do dai khoang thoi gian (vd 15 phut cho dang nhap, 1 phut cho API chung)
   */
  public boolean tryConsume(String key, long capacity, Duration period) {
    return bucket(key, capacity, period).tryConsume(1);
  }

  /**
   * Nhu {@link #tryConsume} nhung tra ve ca so nano-giay con lai truoc khi bucket co luot moi
   * (ConsumptionProbe.getNanosToWaitForRefill()) — dung de dat header Retry-After chinh xac thay vi
   * doan chung chung theo do dai ca so (Prompt #3: bat lai ApiRateLimitFilter phan tang).
   */
  public ConsumptionProbe consume(String key, long capacity, Duration period) {
    return bucket(key, capacity, period).tryConsumeAndReturnRemaining(1);
  }

  /**
   * Hoan lai 1 luot da tru boi {@link #tryConsume} — dung cho truong hop gioi han chi nham chan mot
   * loai "lan thu" cu the (vd dang nhap SAI) nhung tryConsume() phai chay TRUOC khi biet ket qua
   * (de chan brute-force ngay ca khi toan la lan thu dung). Khong vuot qua capacity ban dau
   * (addTokens tu gioi han o muc Bandwidth, khac forceAddTokens).
   *
   * @param key phai giong khoa da dung o tryConsume tuong ung
   * @param capacity phai giong capacity da dung o tryConsume tuong ung (dinh danh cung Bandwidth)
   * @param period phai giong period da dung o tryConsume tuong ung
   */
  public void refund(String key, long capacity, Duration period) {
    bucket(key, capacity, period).addTokens(1);
  }

  private BucketProxy bucket(String key, long capacity, Duration period) {
    byte[] bucketKey = key.getBytes(StandardCharsets.UTF_8);
    BucketConfiguration configuration =
        BucketConfiguration.builder().addLimit(Bandwidth.simple(capacity, period)).build();
    return proxyManager.builder().build(bucketKey, configuration);
  }
}
