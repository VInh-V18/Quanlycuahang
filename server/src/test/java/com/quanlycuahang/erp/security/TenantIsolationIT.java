package com.quanlycuahang.erp.security;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.common.exception.PermissionDeniedException;
import com.quanlycuahang.erp.common.exception.ResourceNotFoundException;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemPriceUpdateRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderRequest;
import com.quanlycuahang.erp.inventory.dto.StockTakeCreateRequest;
import com.quanlycuahang.erp.inventory.service.PurchaseOrderService;
import com.quanlycuahang.erp.inventory.service.StockTakeService;
import com.quanlycuahang.erp.operation.dto.CloseShiftRequest;
import com.quanlycuahang.erp.operation.dto.OpenShiftRequest;
import com.quanlycuahang.erp.operation.dto.ShiftDetailResponse;
import com.quanlycuahang.erp.operation.service.InvoiceDetailService;
import com.quanlycuahang.erp.operation.service.ShiftService;
import com.quanlycuahang.erp.partner.dto.DebtPaymentRequest;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.service.DebtService;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prompt #4 (P1 — audit cách ly tenant + IDOR): bộ test cách ly tenant GIỮ VĨNH VIỄN trong CI —
 * tenant A gọi các endpoint theo {id} bằng ID thuộc tenant B PHẢI luôn 404
 * (ResourceNotFoundException), TUYỆT ĐỐI không lấy được dữ liệu (200). Cơ chế bảo vệ hệ thống đã có
 * sẵn từ trước ({@link com.quanlycuahang.erp.common.repository.TenantAwareRepositoryImpl} override
 * findById/existsById/findAllById/getReferenceById cho MỌI Entity kế thừa TenantScopedEntity, và
 * {@link com.quanlycuahang.erp.auth.security.BranchAccessGuard} cho branchId) — các test dưới đây
 * XÁC NHẬN cơ chế đó hoạt động đúng trên từng luồng nghiệp vụ cụ thể mà roadmap liệt kê (orders,
 * invoices, purchase-orders, stock-takes, shifts, debts), không phải test cơ chế chung lần nữa.
 * (Prompt #10: bỏ case "customer getById" — endpoint GET /customers/{id} đã bị xoá vì không FE nào
 * gọi tới, xem PROJECT_STATE.md mục "Prompt #10"; cơ chế TenantAwareRepositoryImpl vẫn được xác
 * nhận đầy đủ qua các entity khác trong file này.)
 */
class TenantIsolationIT extends AbstractIntegrationTest {

  @Autowired private OrderService orderService;
  @Autowired private InvoiceDetailService invoiceDetailService;
  @Autowired private PurchaseOrderService purchaseOrderService;
  @Autowired private StockTakeService stockTakeService;
  @Autowired private ShiftService shiftService;
  @Autowired private DebtService debtService;
  @Autowired private TestDataFactory testDataFactory;

  private OrderResponse createOrderAsTenant(TestDataFactory.TestTenant tenant, Product product) {
    actingAsUser(tenant.owner());
    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(tenant.branchA().getId());
    OrderLineRequest line = new OrderLineRequest();
    line.setProductId(product.getId());
    line.setQuantity(BigDecimal.ONE);
    line.setLineDiscountAmount(BigDecimal.ZERO);
    request.setLines(List.of(line));
    request.setExpectedTotalAmount(product.getSellPrice());
    OrderPaymentRequest payment = new OrderPaymentRequest();
    payment.setMethod("cash");
    payment.setAmount(product.getSellPrice());
    request.setPayments(List.of(payment));
    return orderService.createOrder(request, "idem-isolation-" + System.nanoTime());
  }

  @Test
  void orderGetByIdAcrossTenantThrowsNotFound() {
    TestDataFactory.TestTenant tenantB =
        testDataFactory.createTenantWithBranches("tenantIsoOrderB");
    Product product =
        testDataFactory.createProductWithStock(
            tenantB.tenant(),
            tenantB.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.TEN);
    OrderResponse orderOfTenantB = createOrderAsTenant(tenantB, product);
    unbindCurrentThread();

    TestDataFactory.TestTenant tenantA =
        testDataFactory.createTenantWithBranches("tenantIsoOrderA");
    actingAsUser(tenantA.owner());

    assertThatThrownBy(() -> orderService.getById(orderOfTenantB.getId()))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void invoiceGetByIdAcrossTenantThrowsNotFound() {
    TestDataFactory.TestTenant tenantB = testDataFactory.createTenantWithBranches("tenantIsoInvB");
    Product product =
        testDataFactory.createProductWithStock(
            tenantB.tenant(),
            tenantB.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.TEN);
    OrderResponse orderOfTenantB = createOrderAsTenant(tenantB, product);
    Long invoiceIdOfTenantB = orderOfTenantB.getInvoiceId();
    unbindCurrentThread();

    TestDataFactory.TestTenant tenantA = testDataFactory.createTenantWithBranches("tenantIsoInvA");
    actingAsUser(tenantA.owner());

    assertThatThrownBy(() -> invoiceDetailService.getById(invoiceIdOfTenantB))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void purchaseOrderGetByIdAndUpdateItemPriceAcrossTenantThrowNotFound() {
    TestDataFactory.TestTenant tenantB = testDataFactory.createTenantWithBranches("tenantIsoPoB");
    actingAsUser(tenantB.owner());
    Product product =
        testDataFactory.createProductWithStock(
            tenantB.tenant(),
            tenantB.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    Supplier supplier = testDataFactory.createSupplier(tenantB.tenant());
    PurchaseOrderRequest request = new PurchaseOrderRequest();
    request.setSupplierId(supplier.getId());
    request.setBranchId(tenantB.branchA().getId());
    PurchaseOrderItemRequest item = new PurchaseOrderItemRequest();
    item.setProductId(product.getId());
    item.setQuantity(BigDecimal.TEN);
    item.setUnitPrice(BigDecimal.valueOf(30_000));
    request.setItems(List.of(item));
    request.setPaidAmount(BigDecimal.ZERO);
    var created = purchaseOrderService.create(request);
    Long purchaseOrderIdOfTenantB = created.getId();
    Long itemIdOfTenantB = created.getItems().get(0).getId();
    unbindCurrentThread();

    TestDataFactory.TestTenant tenantA = testDataFactory.createTenantWithBranches("tenantIsoPoA");
    actingAsUser(tenantA.owner());

    assertThatThrownBy(() -> purchaseOrderService.getById(purchaseOrderIdOfTenantB))
        .isInstanceOf(ResourceNotFoundException.class);

    PurchaseOrderItemPriceUpdateRequest priceUpdate = new PurchaseOrderItemPriceUpdateRequest();
    priceUpdate.setUnitPrice(BigDecimal.valueOf(99_999));
    priceUpdate.setReason("Thu nghiem IDOR xuyen tenant");
    assertThatThrownBy(() -> purchaseOrderService.updateItemPrice(itemIdOfTenantB, priceUpdate))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void stockTakeGetByIdAndApproveAcrossTenantThrowNotFound() {
    TestDataFactory.TestTenant tenantB = testDataFactory.createTenantWithBranches("tenantIsoStB");
    actingAsUser(tenantB.owner());
    StockTakeCreateRequest request = new StockTakeCreateRequest();
    request.setBranchId(tenantB.branchA().getId());
    var created = stockTakeService.create(request);
    Long stockTakeIdOfTenantB = created.getId();
    unbindCurrentThread();

    TestDataFactory.TestTenant tenantA = testDataFactory.createTenantWithBranches("tenantIsoStA");
    actingAsUser(tenantA.owner());

    assertThatThrownBy(() -> stockTakeService.getById(stockTakeIdOfTenantB))
        .isInstanceOf(ResourceNotFoundException.class);
    assertThatThrownBy(() -> stockTakeService.approve(stockTakeIdOfTenantB))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void shiftGetDetailAndCloseAcrossTenantThrowNotFound() {
    TestDataFactory.TestTenant tenantB =
        testDataFactory.createTenantWithBranches("tenantIsoShiftB");
    actingAsUser(tenantB.owner());
    OpenShiftRequest openRequest = new OpenShiftRequest();
    openRequest.setOpeningCash(BigDecimal.valueOf(200_000));
    ShiftDetailResponse shiftOfTenantB = shiftService.open(openRequest);
    unbindCurrentThread();

    TestDataFactory.TestTenant tenantA =
        testDataFactory.createTenantWithBranches("tenantIsoShiftA");
    actingAsUser(tenantA.owner());

    assertThatThrownBy(() -> shiftService.getDetail(shiftOfTenantB.getId()))
        .isInstanceOf(ResourceNotFoundException.class);

    CloseShiftRequest closeRequest = new CloseShiftRequest();
    closeRequest.setActualCash(BigDecimal.valueOf(200_000));
    assertThatThrownBy(() -> shiftService.close(shiftOfTenantB.getId(), closeRequest))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void debtRecordPaymentWithCustomerFromAnotherTenantThrowsNotFound() {
    TestDataFactory.TestTenant tenantB = testDataFactory.createTenantWithBranches("tenantIsoDebtB");
    actingAsUser(tenantB.owner());
    Customer customerOfTenantB =
        testDataFactory.createCustomerWithDebtLimit(
            tenantB.tenant(), BigDecimal.valueOf(5_000_000));
    unbindCurrentThread();

    TestDataFactory.TestTenant tenantA = testDataFactory.createTenantWithBranches("tenantIsoDebtA");
    actingAsUser(tenantA.owner());

    DebtPaymentRequest request = new DebtPaymentRequest();
    request.setDirection("receivable");
    request.setPartnerId(customerOfTenantB.getId());
    request.setAmount(BigDecimal.valueOf(10_000));
    request.setMethod("cash");

    assertThatThrownBy(() -> debtService.recordPayment(request))
        .isInstanceOf(ResourceNotFoundException.class);
  }

  @Test
  void branchAccessGuardRejectsBranchIdBelongingToAnotherTenantWithoutLeakingExistence() {
    TestDataFactory.TestTenant tenantB =
        testDataFactory.createTenantWithBranches("tenantIsoBranchB");
    Long branchIdOfTenantB = tenantB.branchA().getId();
    unbindCurrentThread();

    TestDataFactory.TestTenant tenantA =
        testDataFactory.createTenantWithBranches("tenantIsoBranchA");
    actingAsUser(tenantA.owner());

    // OrderService.createOrder() goi branchAccessGuard.assertAccess() dau tien - dung lam duong
    // vao de kiem tra PermissionDeniedException (khong phai ResourceNotFoundException) va thong
    // diep KHONG tiet lo chi nhanh do co ton tai o tenant khac hay khong.
    OrderCreateRequest request = new OrderCreateRequest();
    request.setBranchId(branchIdOfTenantB);
    request.setLines(List.of());
    request.setExpectedTotalAmount(BigDecimal.ZERO);
    request.setPayments(List.of());

    assertThatThrownBy(
            () -> orderService.createOrder(request, "idem-branch-idor-" + System.nanoTime()))
        .isInstanceOf(PermissionDeniedException.class)
        .hasMessageContaining("Không tìm thấy chi nhánh");
  }
}
