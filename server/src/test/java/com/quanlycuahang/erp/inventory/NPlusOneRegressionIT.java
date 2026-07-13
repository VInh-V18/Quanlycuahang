package com.quanlycuahang.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderResponse;
import com.quanlycuahang.erp.inventory.dto.StockTakeCreateRequest;
import com.quanlycuahang.erp.inventory.dto.StockTakeResponse;
import com.quanlycuahang.erp.inventory.service.InventoryService;
import com.quanlycuahang.erp.inventory.service.PurchaseOrderService;
import com.quanlycuahang.erp.inventory.service.StockTakeService;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.system.entity.Branch;
import com.quanlycuahang.erp.system.entity.Tenant;
import jakarta.persistence.EntityManagerFactory;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;

/**
 * Prompt #7 (P2, hieu nang) - test do bang Hibernate Statistics (SELECT statement count) thay vi
 * doan mo hinh: seed du lieu VUOT qua default_batch_fetch_size=50 (60 dong) roi assert so cau
 * SELECT KHONG ti le thuan voi so dong - neu ai vo tinh xoa JOIN FETCH da them o
 * StockTakeItemRepository/PurchaseOrderItemRepository/InventoryRepository/UserRepository (xem cac
 * repository do), test nay se do duoc >= 60 cau SELECT rieng le va fail ngay thay vi cham cuc bo
 * production o quy mo that (hang chuc nghin SKU/tenant).
 */
@TestPropertySource(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
class NPlusOneRegressionIT extends AbstractIntegrationTest {

  private static final int ROW_COUNT = 60; // > default_batch_fetch_size (50)
  private static final long MAX_SELECT_STATEMENTS =
      15; // vai cau co dinh, KHONG ti le voi ROW_COUNT

  @Autowired private TestDataFactory testDataFactory;
  @Autowired private StockTakeService stockTakeService;
  @Autowired private PurchaseOrderService purchaseOrderService;
  @Autowired private InventoryService inventoryService;
  @Autowired private EntityManagerFactory entityManagerFactory;

  private Statistics statistics() {
    return entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
  }

  @Test
  void stockTakeGetByIdDoesNotIssueOneSelectPerItem() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantNPlusOneSt");
    actingAsUser(tenant.owner());
    for (int i = 0; i < ROW_COUNT; i++) {
      testDataFactory.createProductWithStock(
          tenant.tenant(),
          tenant.branchA(),
          BigDecimal.valueOf(10_000),
          BigDecimal.valueOf(5_000),
          BigDecimal.TEN);
    }

    StockTakeCreateRequest createRequest = new StockTakeCreateRequest();
    createRequest.setBranchId(tenant.branchA().getId());
    StockTakeResponse created = stockTakeService.create(createRequest);
    assertThat(created.getItems()).hasSize(ROW_COUNT);

    Statistics stats = statistics();
    stats.clear();
    StockTakeResponse fetched = stockTakeService.getById(created.getId());
    assertThat(fetched.getItems()).hasSize(ROW_COUNT);

    assertThat(stats.getPrepareStatementCount())
        .as(
            "So cau SELECT khi xem chi tiet 1 phieu kiem ke %d dong (phai KHONG ti le voi so dong"
                + " nho JOIN FETCH product)",
            ROW_COUNT)
        .isLessThanOrEqualTo(MAX_SELECT_STATEMENTS);
  }

  @Test
  void purchaseOrderGetByIdDoesNotIssueOneSelectPerItem() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantNPlusOnePo");
    actingAsUser(tenant.owner());
    Supplier supplier = testDataFactory.createSupplier(tenant.tenant());

    List<PurchaseOrderItemRequest> items = new ArrayList<>(ROW_COUNT);
    for (int i = 0; i < ROW_COUNT; i++) {
      Product product =
          testDataFactory.createProductWithStock(
              tenant.tenant(),
              tenant.branchA(),
              BigDecimal.valueOf(10_000),
              BigDecimal.valueOf(5_000),
              BigDecimal.ZERO);
      PurchaseOrderItemRequest item = new PurchaseOrderItemRequest();
      item.setProductId(product.getId());
      item.setQuantity(BigDecimal.TEN);
      item.setUnitPrice(BigDecimal.valueOf(5_000));
      items.add(item);
    }

    PurchaseOrderRequest request = new PurchaseOrderRequest();
    request.setSupplierId(supplier.getId());
    request.setBranchId(tenant.branchA().getId());
    request.setItems(items);
    request.setPaidAmount(BigDecimal.ZERO);
    request.setDiscountAmount(BigDecimal.ZERO);
    PurchaseOrderResponse created = purchaseOrderService.create(request);
    assertThat(created.getItems()).hasSize(ROW_COUNT);

    Statistics stats = statistics();
    stats.clear();
    PurchaseOrderResponse fetched = purchaseOrderService.getById(created.getId());
    assertThat(fetched.getItems()).hasSize(ROW_COUNT);

    assertThat(stats.getPrepareStatementCount())
        .as(
            "So cau SELECT khi xem chi tiet 1 phieu nhap %d dong (phai KHONG ti le voi so dong nho"
                + " JOIN FETCH product)",
            ROW_COUNT)
        .isLessThanOrEqualTo(MAX_SELECT_STATEMENTS);
  }

  @Test
  void inventoryListByBranchDoesNotIssueOneSelectPerRow() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantNPlusOneInv");
    actingAsUser(tenant.owner());
    Branch branch = tenant.branchA();
    Tenant t = tenant.tenant();
    for (int i = 0; i < ROW_COUNT; i++) {
      testDataFactory.createProductWithStock(
          t, branch, BigDecimal.valueOf(10_000), BigDecimal.valueOf(5_000), BigDecimal.TEN);
    }

    Statistics stats = statistics();
    stats.clear();
    var response = inventoryService.listByBranch(branch.getId(), PageRequest.of(0, ROW_COUNT));
    assertThat(response.getData()).hasSize(ROW_COUNT);

    assertThat(stats.getPrepareStatementCount())
        .as(
            "So cau SELECT khi liet ke ton kho %d dong (phai KHONG ti le voi so dong nho JOIN FETCH"
                + " product)",
            ROW_COUNT)
        .isLessThanOrEqualTo(MAX_SELECT_STATEMENTS);
  }
}
