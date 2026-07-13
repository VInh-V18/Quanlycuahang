package com.quanlycuahang.erp.common.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

/**
 * Metric nghiep vu qua Micrometer (Prompt #8, P2 quan sat) - xuat qua /actuator/prometheus. Moi
 * metric gan tag tenantId (String, khong phai Long) de loc/tach theo tung cua hang tren
 * Prometheus/Grafana — dung "unknown" cho cac truong hop chua xac dinh duoc tenant (vd dang nhap
 * sai voi username khong ton tai).
 */
@Component
public class BusinessMetrics {

  private static final String TAG_TENANT_ID = "tenantId";
  private static final String UNKNOWN_TENANT = "unknown";

  private final MeterRegistry registry;
  private final ConcurrentHashMap<Long, AtomicLong> reconciliationFindingsOpenByTenant =
      new ConcurrentHashMap<>();

  public BusinessMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void recordOrderCreated(Long tenantId) {
    Counter.builder("orders_created_total")
        .description("So don hang duoc tao thanh cong")
        .tags(Tags.of(TAG_TENANT_ID, tenantIdTag(tenantId)))
        .register(registry)
        .increment();
  }

  public Timer.Sample startCheckoutTimer() {
    return Timer.start(registry);
  }

  public void stopCheckoutTimer(Timer.Sample sample, Long tenantId) {
    sample.stop(
        Timer.builder("order_checkout_duration")
            .description("Thoi gian xu ly 1 lan checkout (OrderService.createOrder)")
            .tags(Tags.of(TAG_TENANT_ID, tenantIdTag(tenantId)))
            .register(registry));
  }

  public void recordPriceMismatch(Long tenantId) {
    Counter.builder("order_price_mismatch_total")
        .description("So lan FE/BE tinh tien lech nhau luc checkout (ORDER_PRICE_MISMATCH)")
        .tags(Tags.of(TAG_TENANT_ID, tenantIdTag(tenantId)))
        .register(registry)
        .increment();
  }

  public void recordRateLimitRejected(Long tenantId, String tier) {
    Counter.builder("rate_limit_rejected_total")
        .description("So request bi chan vi vuot rate limit")
        .tags(Tags.of(TAG_TENANT_ID, tenantIdTag(tenantId), "tier", tier))
        .register(registry)
        .increment();
  }

  public void recordLoginFailed(Long tenantId, String reason) {
    Counter.builder("login_failed_total")
        .description("So lan dang nhap that bai (sai mat khau hoac vuot nguong rate limit)")
        .tags(Tags.of(TAG_TENANT_ID, tenantIdTag(tenantId), "reason", reason))
        .register(registry)
        .increment();
  }

  /**
   * Gauge (khong phai counter) - Micrometer doc gia tri hien tai cua AtomicLong moi lan Prometheus
   * scrape, nen chi can CAP NHAT gia tri khi co so moi (sau 1 lan chay doi soat, hoac moi lan FE
   * goi /open-count), khong can goi lien tuc.
   */
  public void setReconciliationFindingsOpen(Long tenantId, long count) {
    AtomicLong value =
        reconciliationFindingsOpenByTenant.computeIfAbsent(
            tenantId,
            id -> {
              AtomicLong initial = new AtomicLong();
              io.micrometer.core.instrument.Gauge.builder(
                      "reconciliation_findings_open", initial, AtomicLong::get)
                  .description("So finding doi soat con o trang thai open")
                  .tags(Tags.of(TAG_TENANT_ID, tenantIdTag(id)))
                  .register(registry);
              return initial;
            });
    value.set(count);
  }

  private static String tenantIdTag(Long tenantId) {
    return tenantId == null ? UNKNOWN_TENANT : String.valueOf(tenantId);
  }
}
