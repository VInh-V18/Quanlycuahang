package com.quanlycuahang.erp.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import com.quanlycuahang.erp.operation.repository.InvoiceRepository;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Prompt #1 (P0): luong nghiep vu quan trong nhat he thong -
 * OrderService.createOrder(). Moi test tao tenant/branch/product/customer RIENG qua TestDataFactory
 * de khong dam vao du lieu cua test khac.
 */
class OrderServiceCreateOrderIT extends AbstractIntegrationTest {

  @Autowired private OrderService orderService;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private InventoryTransactionRepository inventoryTransactionRepository;
  @Autowired private OrderPaymentRepository orderPaymentRepository;
  @Autowired private InvoiceRepository invoiceRepository;
  @Autowired private DebtRepository debtRepository;

  private OrderLineRequest line(Long productId, BigDecimal quantity) {
    OrderLineRequest line = new OrderLineRequest();
    line.setProductId(productId);
    line.setQuantity(quantity);
    line.setLineDiscountAmount(BigDecimal.ZERO);
    return line;
  }

  private OrderPaymentRequest payment(String method, BigDecimal amount) {
    OrderPaymentRequest payment = new OrderPaymentRequest();
    payment.setMethod(method);
    payment.setAmount(amount);
    return payment;
  }

  @Test
  void happyPathDeductsStockAndRecordsPaymentAndInvoice() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantOrderHappy");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.valueOf(20));

    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    request.setLines(List.of(line(product.getId(), BigDecimal.valueOf(3))));
    request.setExpectedTotalAmount(BigDecimal.valueOf(150_000));
    request.setPayments(List.of(payment("cash", BigDecimal.valueOf(150_000))));

    OrderResponse response = orderService.createOrder(request, "idem-happy-" + System.nanoTime());

    assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(150_000));
    assertThat(response.getInvoiceId()).isNotNull();
    assertThat(response.getLookupCode()).isNotBlank();

    Inventory inventory =
        inventoryRepository
            .findByProductIdAndBranchId(product.getId(), tenant.branchA().getId())
            .orElseThrow();
    assertThat(inventory.getStock()).isEqualByComparingTo(BigDecimal.valueOf(17));

    assertThat(
            inventoryTransactionRepository.findByProductIdAndBranchIdOrderByCreatedAtDesc(
                product.getId(),
                tenant.branchA().getId(),
                org.springframework.data.domain.Pageable.unpaged()))
        .hasSize(1)
        .allSatisfy(tx -> assertThat(tx.getType()).isEqualTo("sale"));

    assertThat(orderPaymentRepository.findByOrderId(response.getId())).hasSize(1);
    assertThat(invoiceRepository.findByOrderId(response.getId())).isPresent();
  }

  @Test
  void groupsQuantityAcrossTwoLinesOfSameProductBeforeCheckingStock() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantOrderGroup");
    actingAsUser(tenant.owner());
    // Ton kho = 10, 2 dong CUNG 1 san pham 8 + 8 = 16 > 10 -> phai bi chan (khong duoc qua vi moi
    // dong tu so voi ton CHUA tru).
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(10_000),
            BigDecimal.valueOf(5_000),
            BigDecimal.TEN);

    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    request.setLines(
        List.of(
            line(product.getId(), BigDecimal.valueOf(8)),
            line(product.getId(), BigDecimal.valueOf(8))));
    request.setExpectedTotalAmount(BigDecimal.valueOf(160_000));
    request.setPayments(List.of(payment("cash", BigDecimal.valueOf(160_000))));

    assertThatThrownBy(() -> orderService.createOrder(request, "idem-group-" + System.nanoTime()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("chỉ còn");
  }

  @Test
  void blocksUnpaidOrderWithoutCustomer() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantOrderDebtNoCust");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.TEN);

    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    request.setLines(List.of(line(product.getId(), BigDecimal.ONE)));
    request.setExpectedTotalAmount(BigDecimal.valueOf(50_000));
    request.setPayments(List.of()); // khong thanh toan gi -> unpaid = 50.000, khong co customer

    assertThatThrownBy(() -> orderService.createOrder(request, "idem-nocust-" + System.nanoTime()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("khách hàng");
  }

  @Test
  void blocksOrderExceedingCustomerDebtLimit() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantOrderDebtLimit");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(200_000),
            BigDecimal.valueOf(100_000),
            BigDecimal.TEN);
    Customer customer =
        testDataFactory.createCustomerWithDebtLimit(tenant.tenant(), BigDecimal.valueOf(100_000));

    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    request.setCustomerId(customer.getId());
    request.setLines(List.of(line(product.getId(), BigDecimal.ONE)));
    request.setExpectedTotalAmount(BigDecimal.valueOf(200_000));
    request.setPayments(List.of()); // no vuot han muc 100.000 vi tong don la 200.000

    assertThatThrownBy(
            () -> orderService.createOrder(request, "idem-debtlimit-" + System.nanoTime()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("hạn mức nợ");
  }

  @Test
  void blocksWhenFrontendExpectedTotalDoesNotMatchBackendCalculation() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantOrderMismatch");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.TEN);

    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    request.setLines(List.of(line(product.getId(), BigDecimal.ONE)));
    request.setExpectedTotalAmount(BigDecimal.valueOf(999_999)); // sai co y
    request.setPayments(List.of(payment("cash", BigDecimal.valueOf(999_999))));

    assertThatThrownBy(
            () -> orderService.createOrder(request, "idem-mismatch-" + System.nanoTime()))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("không khớp");
  }

  @Test
  void concurrentCheckoutsOnLastUnitOfStockOnlyOneSucceeds() throws InterruptedException {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantOrderConcurrent");
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.ONE);

    int threadCount = 5;
    ExecutorService pool = Executors.newFixedThreadPool(threadCount);
    CountDownLatch ready = new CountDownLatch(threadCount);
    CountDownLatch go = new CountDownLatch(1);
    AtomicInteger successCount = new AtomicInteger();
    AtomicInteger failureCount = new AtomicInteger();

    Long tenantId = tenant.tenant().getId();
    for (int i = 0; i < threadCount; i++) {
      pool.submit(
          () -> {
            actingAsUser(tenant.owner());
            ready.countDown();
            try {
              go.await();
              OrderCreateRequest request = new OrderCreateRequest();
              request.setBranchId(tenant.branchA().getId());
              request.setLines(List.of(line(product.getId(), BigDecimal.ONE)));
              request.setExpectedTotalAmount(BigDecimal.valueOf(50_000));
              request.setPayments(List.of(payment("cash", BigDecimal.valueOf(50_000))));
              orderService.createOrder(request, "idem-concurrent-" + java.util.UUID.randomUUID());
              successCount.incrementAndGet();
            } catch (Exception ex) {
              failureCount.incrementAndGet();
            } finally {
              unbindCurrentThread();
            }
          });
    }
    ready.await(5, TimeUnit.SECONDS);
    go.countDown();
    pool.shutdown();
    pool.awaitTermination(30, TimeUnit.SECONDS);

    assertThat(successCount.get()).isEqualTo(1);
    assertThat(failureCount.get()).isEqualTo(threadCount - 1);

    actingAsTenant(tenantId);
    Inventory finalInventory =
        inventoryRepository
            .findByProductIdAndBranchId(product.getId(), tenant.branchA().getId())
            .orElseThrow();
    assertThat(finalInventory.getStock()).isEqualByComparingTo(BigDecimal.ZERO);
  }
}
