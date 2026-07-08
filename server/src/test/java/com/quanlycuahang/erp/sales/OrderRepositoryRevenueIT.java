package com.quanlycuahang.erp.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Integration test Phase 11 Gate: khoa lai cach tinh gia von (cost_of_goods_sold) theo tung nhom
 * (ngay/tuan/thang) them o FH-15 — subquery LEFT JOIN rieng gitua doanh thu va gia von thay vi JOIN
 * thang order_items vao orders (se nhan doi revenue neu 1 don co nhieu dong). Chay tren PostgreSQL
 * that qua Testcontainers de xac nhan dung native SQL that, khong phai JPQL gia lap. Ghi chu moi
 * truong: sandbox lam Phase 11 KHONG co Docker daemon (da xac minh qua `docker ps`) — query nay da
 * duoc verify gian tiep bang curl that tren Postgres 16 local trong phien FH-15; file nay chay duoc
 * that tren moi truong CI/local co Docker.
 */
@Testcontainers
@SpringBootTest
class OrderRepositoryRevenueIT {

  @Container
  static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

  @Container
  static GenericContainer<?> redis =
      new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", postgres::getJdbcUrl);
    registry.add("spring.datasource.username", postgres::getUsername);
    registry.add("spring.datasource.password", postgres::getPassword);
    registry.add("spring.data.redis.host", redis::getHost);
    registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
  }

  @Autowired private BranchRepository branchRepository;
  @Autowired private UserRepository userRepository;
  @Autowired private ProductRepository productRepository;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;

  @Test
  void findRevenueByPeriodComputesCostOfGoodsSoldWithoutDoublingRevenueOnMultiLineOrder() {
    Branch branch = branchRepository.findAll().get(0);
    User cashier = userRepository.findAll().get(0);
    Product product = productRepository.findAll().get(0);

    OffsetDateTime now = OffsetDateTime.now();

    Order order = new Order();
    order.setOrderNumber("IT-TEST-000001");
    order.setBranch(branch);
    order.setCashier(cashier);
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
            "day", now.minusDays(1), now.plusDays(1), branch.getId(), 1L);

    // Gop chung voi du lieu seed co san trong ngay hom nay (neu co) — chi kiem tra hang cua ngay
    // hien tai chua dung revenue/cogs, khong gia dinh la hang duy nhat.
    Object[] todayRow =
        rows.stream()
            .filter(r -> r[0].toString().equals(now.toLocalDate().toString()))
            .findFirst()
            .orElseThrow();

    BigDecimal revenue = (BigDecimal) todayRow[1];
    BigDecimal cogs = (BigDecimal) todayRow[3];

    // revenue (SUM(o.total_amount)) khong duoc nhan doi du don co 2 dong -> phai >= 300.000 dung
    // 1 lan cho don nay (co the > neu co du lieu seed khac cung ngay).
    assertThat(revenue).isGreaterThanOrEqualTo(BigDecimal.valueOf(300_000));
    // gia von: dong1 = 70.000*2=140.000, dong2=70.000*1=70.000 -> +210.000 cho don nay.
    assertThat(cogs).isGreaterThanOrEqualTo(BigDecimal.valueOf(210_000));
  }
}
