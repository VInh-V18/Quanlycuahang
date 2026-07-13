package com.quanlycuahang.erp.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Phase 11 Gate: khoa lai cach tinh gia von (cost_of_goods_sold) theo tung nhom
 * (ngay/tuan/thang) them o FH-15 — subquery LEFT JOIN rieng gitua doanh thu va gia von thay vi JOIN
 * thang order_items vao orders (se nhan doi revenue neu 1 don co nhieu dong). Tao tenant/branch/
 * product rieng qua TestDataFactory (thay vi seed co san) + actingAsTenant() de dung dung ca 2 co
 * che: Hibernate @Filter cho JPA thuong VA tenant_id truyen tay cho native query nay (ban cu KHONG
 * bind TenantContext, chi dua vao seed data tenant #1 duy nhat de "tinh co" dung).
 */
class OrderRepositoryRevenueIT extends AbstractIntegrationTest {

  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private TestDataFactory testDataFactory;

  @Test
  void findRevenueByPeriodComputesCostOfGoodsSoldWithoutDoublingRevenueOnMultiLineOrder() {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantRevenue");
    actingAsTenant(tenant.tenant().getId());

    var product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(100_000),
            BigDecimal.valueOf(70_000),
            BigDecimal.valueOf(100));

    OffsetDateTime now = OffsetDateTime.now();

    Order order = new Order();
    order.setTenant(tenant.tenant());
    order.setOrderNumber("IT-TEST-000001");
    order.setBranch(tenant.branchA());
    order.setCashier(tenant.cashier());
    order.setStatus("completed");
    order.setSubtotalAmount(BigDecimal.valueOf(300_000));
    order.setDiscountAmount(BigDecimal.ZERO);
    order.setVatAmount(BigDecimal.ZERO);
    order.setRoundingAdjustment(BigDecimal.ZERO);
    order.setTotalAmount(BigDecimal.valueOf(300_000));
    Order savedOrder = orderRepository.save(order);

    // 2 dong tren CUNG 1 don — neu JOIN thang order_items vao orders se nhan doi revenue (300.000
    // -> 600.000) do 2 dong khop voi 1 don khi GROUP BY; subquery rieng phai tranh duoc loi nay.
    OrderItem item1 = new OrderItem();
    item1.setTenant(tenant.tenant());
    item1.setOrder(savedOrder);
    item1.setProduct(product);
    item1.setProductNameSnapshot(product.getName());
    item1.setUnitPriceSnapshot(BigDecimal.valueOf(100_000));
    item1.setCostPriceSnapshot(BigDecimal.valueOf(70_000));
    item1.setVatRateSnapshot(BigDecimal.ZERO);
    item1.setQuantity(BigDecimal.valueOf(2));
    item1.setDiscountAmount(BigDecimal.ZERO);
    item1.setVatAmount(BigDecimal.ZERO);
    item1.setLineTotal(BigDecimal.valueOf(200_000));
    orderItemRepository.save(item1);

    OrderItem item2 = new OrderItem();
    item2.setTenant(tenant.tenant());
    item2.setOrder(savedOrder);
    item2.setProduct(product);
    item2.setProductNameSnapshot(product.getName());
    item2.setUnitPriceSnapshot(BigDecimal.valueOf(100_000));
    item2.setCostPriceSnapshot(BigDecimal.valueOf(70_000));
    item2.setVatRateSnapshot(BigDecimal.ZERO);
    item2.setQuantity(BigDecimal.valueOf(1));
    item2.setDiscountAmount(BigDecimal.ZERO);
    item2.setVatAmount(BigDecimal.ZERO);
    item2.setLineTotal(BigDecimal.valueOf(100_000));
    orderItemRepository.save(item2);

    List<Object[]> rows =
        orderRepository.findRevenueByPeriod(
            "day",
            now.minusDays(1),
            now.plusDays(1),
            tenant.branchA().getId(),
            tenant.tenant().getId());

    Object[] todayRow =
        rows.stream()
            .filter(r -> r[0].toString().equals(now.toLocalDate().toString()))
            .findFirst()
            .orElseThrow();

    BigDecimal revenue = (BigDecimal) todayRow[1];
    BigDecimal cogs = (BigDecimal) todayRow[3];

    // Tenant rieng nen KHONG con du lieu khac lan vao - revenue/cogs phai dung CHINH XAC (khong
    // chi >=) vi day la tenant moi tinh, chi co dung 1 don nay.
    assertThat(revenue).isEqualByComparingTo(BigDecimal.valueOf(300_000));
    // gia von: dong1 = 70.000*2=140.000, dong2=70.000*1=70.000 -> 210.000.
    assertThat(cogs).isEqualByComparingTo(BigDecimal.valueOf(210_000));
  }
}
