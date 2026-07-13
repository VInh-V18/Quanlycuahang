package com.quanlycuahang.erp.common.metrics;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.service.OrderService;
import io.micrometer.core.instrument.MeterRegistry;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prompt #8 (P2, quan sat) - gate kiem chung bat buoc cua roadmap: "tao 1 don POS tren moi truong
 * dev -> chi ra duoc... metric orders_created_total tang 1 dung tag tenant". Test nay lam CHINH XAC
 * dieu do bang so that (khong phai anh chup man hinh): tao 1 don qua OrderService.createOrder (dung
 * luong nghiep vu that, khong goi thang MeterRegistry) roi doc lai gia tri counter/timer tu
 * MeterRegistry that (Spring Boot tu dong cau hinh PrometheusMeterRegistry vi co
 * micrometer-registry-prometheus tren classpath) da duoc BusinessMetrics ghi vao.
 */
class BusinessMetricsGateIT extends AbstractIntegrationTest {

  @Autowired private OrderService orderService;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private MeterRegistry meterRegistry;

  @Test
  void creatingOrderIncrementsOrdersCreatedTotalAndRecordsCheckoutDuration() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantMetricsGate");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.TEN);
    String tenantTag = String.valueOf(tenant.tenant().getId());

    double before =
        meterRegistry.find("orders_created_total").tag("tenantId", tenantTag).counter() == null
            ? 0
            : meterRegistry
                .find("orders_created_total")
                .tag("tenantId", tenantTag)
                .counter()
                .count();

    OrderLineRequest line = new OrderLineRequest();
    line.setProductId(product.getId());
    line.setQuantity(BigDecimal.ONE);
    line.setLineDiscountAmount(BigDecimal.ZERO);
    OrderPaymentRequest payment = new OrderPaymentRequest();
    payment.setMethod("cash");
    payment.setAmount(BigDecimal.valueOf(50_000));

    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    request.setLines(List.of(line));
    request.setExpectedTotalAmount(BigDecimal.valueOf(50_000));
    request.setPayments(List.of(payment));

    orderService.createOrder(request, "idem-metrics-gate-" + System.nanoTime());

    double after =
        meterRegistry.find("orders_created_total").tag("tenantId", tenantTag).counter().count();
    assertThat(after).isEqualTo(before + 1);

    assertThat(
            meterRegistry
                .find("order_checkout_duration")
                .tag("tenantId", tenantTag)
                .timer()
                .count())
        .isGreaterThanOrEqualTo(1);
  }
}
