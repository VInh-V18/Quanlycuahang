package com.quanlycuahang.erp.inventory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemPriceUpdateRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderResponse;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.service.PurchaseOrderService;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Prompt #1 (P0): PurchaseOrderService.create() + updateItemPrice() - trong tam
 * cong thuc gia von binh quan gia quyen (AverageCostService) va co che replay khi sua gia 1 dong
 * phieu nhap cu, cong voi cong no NCC phat sinh/dieu chinh dung.
 */
class PurchaseOrderServiceIT extends AbstractIntegrationTest {

  @Autowired private PurchaseOrderService purchaseOrderService;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private InventoryRepository inventoryRepository;
  @Autowired private DebtRepository debtRepository;

  private PurchaseOrderItemRequest item(Long productId, BigDecimal quantity, BigDecimal unitPrice) {
    PurchaseOrderItemRequest item = new PurchaseOrderItemRequest();
    item.setProductId(productId);
    item.setQuantity(quantity);
    item.setUnitPrice(unitPrice);
    return item;
  }

  @Test
  void createComputesWeightedAverageCostAndRecordsPayableDebtWhenUnderpaid() {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantPoCreate");
    actingAsUser(tenant.owner());
    // Ton hien tai 10 @ gia von 60.000.
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(150_000),
            BigDecimal.valueOf(60_000),
            BigDecimal.TEN);
    Supplier supplier = testDataFactory.createSupplier(tenant.tenant());

    PurchaseOrderRequest request = new PurchaseOrderRequest();
    request.setSupplierId(supplier.getId());
    request.setBranchId(tenant.branchA().getId());
    // Nhap them 10 @ 80.000 -> gia von moi = (10*60.000 + 10*80.000) / 20 = 70.000.
    request.setItems(List.of(item(product.getId(), BigDecimal.TEN, BigDecimal.valueOf(80_000))));
    request.setPaidAmount(BigDecimal.valueOf(500_000)); // tra truoc 500.000/800.000 -> no 300.000
    request.setDiscountAmount(BigDecimal.ZERO);

    PurchaseOrderResponse response = purchaseOrderService.create(request);

    assertThat(response.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(800_000));

    Inventory inventory =
        inventoryRepository
            .findByProductIdAndBranchId(product.getId(), tenant.branchA().getId())
            .orElseThrow();
    assertThat(inventory.getStock()).isEqualByComparingTo(BigDecimal.valueOf(20));
    assertThat(inventory.getCostPrice()).isEqualByComparingTo(BigDecimal.valueOf(70_000));

    List<Debt> debts =
        debtRepository.findByReferenceTypeAndReferenceId("purchase_order", response.getId());
    assertThat(debts).hasSize(1);
    assertThat(debts.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(300_000));
    assertThat(debts.get(0).getDirection()).isEqualTo("payable");
  }

  @Test
  void createDoesNotRecordDebtWhenFullyPaid() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantPoFullPaid");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(150_000),
            BigDecimal.valueOf(60_000),
            BigDecimal.ZERO);
    Supplier supplier = testDataFactory.createSupplier(tenant.tenant());

    PurchaseOrderRequest request = new PurchaseOrderRequest();
    request.setSupplierId(supplier.getId());
    request.setBranchId(tenant.branchA().getId());
    request.setItems(List.of(item(product.getId(), BigDecimal.TEN, BigDecimal.valueOf(80_000))));
    request.setPaidAmount(BigDecimal.valueOf(800_000)); // tra du 100%
    request.setDiscountAmount(BigDecimal.ZERO);

    PurchaseOrderResponse response = purchaseOrderService.create(request);

    assertThat(debtRepository.findByReferenceTypeAndReferenceId("purchase_order", response.getId()))
        .isEmpty();
  }

  @Test
  void updateItemPriceReplaysAverageCostAndAdjustsExistingDebt() {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantPoReplay");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(150_000),
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    Supplier supplier = testDataFactory.createSupplier(tenant.tenant());

    PurchaseOrderRequest request = new PurchaseOrderRequest();
    request.setSupplierId(supplier.getId());
    request.setBranchId(tenant.branchA().getId());
    // Nhap sai gia 50.000 (dung ra la 80.000), khong tra dong nao -> no toan bo 500.000.
    request.setItems(List.of(item(product.getId(), BigDecimal.TEN, BigDecimal.valueOf(50_000))));
    request.setPaidAmount(BigDecimal.ZERO);
    request.setDiscountAmount(BigDecimal.ZERO);
    PurchaseOrderResponse created = purchaseOrderService.create(request);
    Long itemId = created.getItems().get(0).getId();

    PurchaseOrderItemPriceUpdateRequest fix = new PurchaseOrderItemPriceUpdateRequest();
    fix.setUnitPrice(BigDecimal.valueOf(80_000));
    fix.setReason("Nhap nham 50.000 thanh 80.000");

    PurchaseOrderResponse updated = purchaseOrderService.updateItemPrice(itemId, fix);

    assertThat(updated.getTotalAmount()).isEqualByComparingTo(BigDecimal.valueOf(800_000));

    Inventory inventory =
        inventoryRepository
            .findByProductIdAndBranchId(product.getId(), tenant.branchA().getId())
            .orElseThrow();
    // Ton khong doi (10), gia von phai theo gia MOI (80.000), khong con la 50.000.
    assertThat(inventory.getStock()).isEqualByComparingTo(BigDecimal.TEN);
    assertThat(inventory.getCostPrice()).isEqualByComparingTo(BigDecimal.valueOf(80_000));

    // No phai tang them dung phan chenh lech: delta = (80.000-50.000)*10 = 300.000 ->
    // 500.000+300.000=800.000.
    List<Debt> debts =
        debtRepository.findByReferenceTypeAndReferenceId("purchase_order", created.getId());
    assertThat(debts).hasSize(1);
    assertThat(debts.get(0).getAmount()).isEqualByComparingTo(BigDecimal.valueOf(800_000));
  }

  @Test
  void updateItemPriceBlocksWhenReductionWouldMakeDebtNegative() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantPoReplayBlock");
    actingAsUser(tenant.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(150_000),
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    Supplier supplier = testDataFactory.createSupplier(tenant.tenant());

    PurchaseOrderRequest request = new PurchaseOrderRequest();
    request.setSupplierId(supplier.getId());
    request.setBranchId(tenant.branchA().getId());
    request.setItems(List.of(item(product.getId(), BigDecimal.TEN, BigDecimal.valueOf(80_000))));
    // Tra truoc gan het, chi con no 50.000.
    request.setPaidAmount(BigDecimal.valueOf(750_000));
    request.setDiscountAmount(BigDecimal.ZERO);
    PurchaseOrderResponse created = purchaseOrderService.create(request);
    Long itemId = created.getItems().get(0).getId();

    PurchaseOrderItemPriceUpdateRequest reduce = new PurchaseOrderItemPriceUpdateRequest();
    // Giam gia con 10.000/don vi -> delta = (10.000-80.000)*10 = -700.000, vuot qua no con lai
    // (50.000) -> phai bi chan thay vi cho no am.
    reduce.setUnitPrice(BigDecimal.valueOf(10_000));
    reduce.setReason("Thu nghiem giam gia vuot no con lai");

    assertThatThrownBy(() -> purchaseOrderService.updateItemPrice(itemId, reduce))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("công nợ còn lại");
  }
}
