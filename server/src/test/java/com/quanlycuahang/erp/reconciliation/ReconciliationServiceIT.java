package com.quanlycuahang.erp.reconciliation;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderRequest;
import com.quanlycuahang.erp.inventory.service.PurchaseOrderService;
import com.quanlycuahang.erp.operation.dto.CloseShiftRequest;
import com.quanlycuahang.erp.operation.dto.OpenShiftRequest;
import com.quanlycuahang.erp.operation.dto.ShiftDetailResponse;
import com.quanlycuahang.erp.operation.entity.Shift;
import com.quanlycuahang.erp.operation.repository.ShiftRepository;
import com.quanlycuahang.erp.operation.service.ShiftService;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.entity.DebtPayment;
import com.quanlycuahang.erp.partner.repository.DebtPaymentRepository;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.reconciliation.dto.ReconciliationRunResponse;
import com.quanlycuahang.erp.reconciliation.entity.ReconciliationRun;
import com.quanlycuahang.erp.reconciliation.service.ReconciliationService;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.entity.Order;
import com.quanlycuahang.erp.sales.entity.OrderItem;
import com.quanlycuahang.erp.sales.entity.OrderPayment;
import com.quanlycuahang.erp.sales.entity.Return;
import com.quanlycuahang.erp.sales.entity.ReturnItem;
import com.quanlycuahang.erp.sales.repository.OrderItemRepository;
import com.quanlycuahang.erp.sales.repository.OrderPaymentRepository;
import com.quanlycuahang.erp.sales.repository.OrderRepository;
import com.quanlycuahang.erp.sales.repository.ReturnItemRepository;
import com.quanlycuahang.erp.sales.repository.ReturnRepository;
import com.quanlycuahang.erp.sales.service.OrderService;
import com.quanlycuahang.erp.sales.statemachine.OrderStatus;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Prompt #6 (P1): xac nhan ca 7 phep doi soat (a-g) phat hien dung loai lech khi du lieu CO Y bi
 * pha vo, VA khong bao gio bao sai (false positive) tren du lieu xay dung qua cac Service THAT
 * (khong phai insert tay). Moi cau SQL trong ReconciliationService la 1 aggregate DUY NHAT (khong
 * N+1) - xem Javadoc ReconciliationService ve 2 diem dieu chinh cong thuc so voi mo ta goc.
 *
 * <p>INVOICE_DUPLICATE (1 nhanh cua muc d) KHONG co test rieng: DB co {@code uq_invoices_order_id}
 * la UNIQUE CONSTRAINT THUONG (khong loc deleted_at), nen KHONG THE nao insert 2 dong invoice cung
 * order_id du la cach nao (kiem tra bang code, khong doan) - nhanh nay ton tai thuan tuy phong thu,
 * khong co duong nao trong ung dung (hay ca insert tay trong test) tao ra duoc tinh huong do.
 */
class ReconciliationServiceIT extends AbstractIntegrationTest {

  @Autowired private ReconciliationService reconciliationService;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private OrderService orderService;
  @Autowired private PurchaseOrderService purchaseOrderService;
  @Autowired private ShiftService shiftService;
  @Autowired private OrderRepository orderRepository;
  @Autowired private OrderItemRepository orderItemRepository;
  @Autowired private ReturnRepository returnRepository;
  @Autowired private ReturnItemRepository returnItemRepository;
  @Autowired private DebtRepository debtRepository;
  @Autowired private DebtPaymentRepository debtPaymentRepository;
  @Autowired private ShiftRepository shiftRepository;
  @Autowired private OrderPaymentRepository orderPaymentRepository;

  @Test
  void detectsAllSevenCheckTypesOnDeliberatelyBrokenData() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantReconDirty");
    actingAsUser(tenant.owner());

    // (a) INVENTORY_MISMATCH: Inventory duoc TestDataFactory tao THANG voi stock=10 nhung KHONG
    // co dong inventory_transactions nao di kem (khac han di qua PurchaseOrderService that) ->
    // stock(10) != tong giao dich (0).
    testDataFactory.createProductWithStock(
        tenant.tenant(),
        tenant.branchA(),
        BigDecimal.valueOf(50_000),
        BigDecimal.valueOf(30_000),
        BigDecimal.TEN);

    // (b) DEBT_MISMATCH: ghi 1 khoan thanh toan debt_payments nhung KHONG tru vao debts.amount -
    // paid(50.000) > (goc(100.000) - con lai(100.000) = 0).
    Debt debt = new Debt();
    debt.setTenant(tenant.tenant());
    debt.setCustomer(testDataFactory.createCustomerWithDebtLimit(tenant.tenant(), BigDecimal.ZERO));
    debt.setDirection("receivable");
    debt.setAmount(BigDecimal.valueOf(100_000));
    debt.setOriginalAmount(BigDecimal.valueOf(100_000));
    debt = debtRepository.save(debt);
    DebtPayment payment = new DebtPayment();
    payment.setTenant(tenant.tenant());
    payment.setDebt(debt);
    payment.setAmount(BigDecimal.valueOf(50_000));
    payment.setMethod("cash");
    payment.setPaidAt(OffsetDateTime.now());
    debtPaymentRepository.save(payment);

    // (c) ORDER_TOTAL_MISMATCH + (d) INVOICE_MISSING + (e) RETURN_OVER_QUANTITY: 1 don hang dung
    // tay (khong qua OrderService) de tong tien KHONG khop tong dong, KHONG co hoa don, va co
    // return_items vuot qua so luong da mua.
    Order brokenOrder = new Order();
    brokenOrder.setTenant(tenant.tenant());
    brokenOrder.setOrderNumber("IT-RECON-000001");
    brokenOrder.setBranch(tenant.branchA());
    brokenOrder.setCashier(tenant.owner());
    brokenOrder.setStatus(OrderStatus.COMPLETED.getValue());
    brokenOrder.setSubtotalAmount(BigDecimal.valueOf(50_000));
    brokenOrder.setDiscountAmount(BigDecimal.ZERO);
    brokenOrder.setVatAmount(BigDecimal.ZERO);
    brokenOrder.setRoundingAdjustment(BigDecimal.ZERO);
    brokenOrder.setShippingFee(BigDecimal.ZERO);
    // Co y sai: total_amount ghi 100.000 nhung dong hang ben duoi chi co 50.000 -> lech.
    brokenOrder.setTotalAmount(BigDecimal.valueOf(100_000));
    brokenOrder = orderRepository.save(brokenOrder);

    OrderItem item = new OrderItem();
    item.setTenant(tenant.tenant());
    item.setOrder(brokenOrder);
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.valueOf(30_000),
            BigDecimal.TEN);
    item.setProduct(product);
    item.setProductNameSnapshot(product.getName());
    item.setUnitPriceSnapshot(BigDecimal.valueOf(50_000));
    item.setCostPriceSnapshot(BigDecimal.valueOf(30_000));
    item.setVatRateSnapshot(BigDecimal.ZERO);
    item.setQuantity(BigDecimal.valueOf(5));
    item.setDiscountAmount(BigDecimal.ZERO);
    item.setVatAmount(BigDecimal.ZERO);
    item.setLineTotal(BigDecimal.valueOf(50_000));
    item.setReturnedQuantity(
        BigDecimal.ZERO); // cot cache noi "chua tra gi" - se lech voi return_items ben duoi
    item = orderItemRepository.save(item);
    // KHONG tao Invoice cho don nay -> INVOICE_MISSING.

    Return returnEntity = new Return();
    returnEntity.setTenant(tenant.tenant());
    returnEntity.setOrder(brokenOrder);
    returnEntity.setCreatedBy(tenant.owner());
    returnEntity.setRefundMethod("cash");
    returnEntity.setTotalRefund(BigDecimal.valueOf(100_000));
    returnEntity = returnRepository.save(returnEntity);
    ReturnItem returnItem = new ReturnItem();
    returnItem.setTenant(tenant.tenant());
    returnItem.setReturnEntity(returnEntity);
    returnItem.setOrderItem(item);
    // Tra VUOT so luong da mua (5) -> RETURN_OVER_QUANTITY.
    returnItem.setQuantity(BigDecimal.valueOf(10));
    returnItem.setRefundAmount(BigDecimal.valueOf(100_000));
    returnItemRepository.save(returnItem);

    // (f) ORPHANED_REFERENCE: Debt tro toi 1 order_id khong ton tai.
    Debt orphanDebt = new Debt();
    orphanDebt.setTenant(tenant.tenant());
    orphanDebt.setCustomer(
        testDataFactory.createCustomerWithDebtLimit(tenant.tenant(), BigDecimal.ZERO));
    orphanDebt.setDirection("receivable");
    orphanDebt.setAmount(BigDecimal.valueOf(10_000));
    orphanDebt.setOriginalAmount(BigDecimal.valueOf(10_000));
    orphanDebt.setReferenceType("order");
    orphanDebt.setReferenceId(999_999_999L);
    debtRepository.save(orphanDebt);

    // (g) SHIFT_DISCREPANCY_MISMATCH: ca da dong voi actual_cash/discrepancy KHONG khop cong thuc
    // tinh lai (thieu 1 khoan ban tien mat 50.000 khong duoc tinh vao luc dong ca that). Mo ca qua
    // TestDataFactory (chi tao trang thai "open"), roi tu sua thanh "closed" voi so lieu co y lech
    // - PHAI luu lai qua repository, sua truong tren Java entity khong tu ghi xuong DB.
    Shift savedShift =
        testDataFactory.openShift(
            tenant.tenant(), tenant.branchA(), tenant.owner(), BigDecimal.valueOf(100_000));
    savedShift.setStatus("closed");
    savedShift.setActualCash(BigDecimal.valueOf(100_000));
    savedShift.setDiscrepancy(
        BigDecimal.ZERO); // ham y expected=100.000, nhung don ben duoi cong them 50.000 tien mat
    savedShift.setClosedAt(OffsetDateTime.now().plusHours(1));
    savedShift = shiftRepository.save(savedShift);

    Order cashOrderInShift = new Order();
    cashOrderInShift.setTenant(tenant.tenant());
    cashOrderInShift.setOrderNumber("IT-RECON-000002");
    cashOrderInShift.setBranch(tenant.branchA());
    cashOrderInShift.setCashier(tenant.owner());
    cashOrderInShift.setShift(savedShift);
    cashOrderInShift.setStatus(OrderStatus.COMPLETED.getValue());
    cashOrderInShift.setSubtotalAmount(BigDecimal.valueOf(50_000));
    cashOrderInShift.setDiscountAmount(BigDecimal.ZERO);
    cashOrderInShift.setVatAmount(BigDecimal.ZERO);
    cashOrderInShift.setRoundingAdjustment(BigDecimal.ZERO);
    cashOrderInShift.setShippingFee(BigDecimal.ZERO);
    cashOrderInShift.setTotalAmount(BigDecimal.valueOf(50_000));
    cashOrderInShift = orderRepository.save(cashOrderInShift);
    OrderPayment cashPayment = new OrderPayment();
    cashPayment.setTenant(tenant.tenant());
    cashPayment.setOrder(cashOrderInShift);
    cashPayment.setMethod("cash");
    cashPayment.setAmount(BigDecimal.valueOf(50_000));
    orderPaymentRepository.save(cashPayment);

    ReconciliationRunResponse result =
        reconciliationService.runForTenant(
            tenant.tenant().getId(), ReconciliationRun.TRIGGER_MANUAL, null);

    assertThat(result.getStatus()).isEqualTo(ReconciliationRun.STATUS_COMPLETED);
    Set<String> checkTypes =
        result.getFindings().stream().map(f -> f.getCheckType()).collect(Collectors.toSet());
    assertThat(checkTypes)
        .contains(
            "INVENTORY_MISMATCH",
            "DEBT_MISMATCH",
            "ORDER_TOTAL_MISMATCH",
            "INVOICE_MISSING",
            "RETURN_OVER_QUANTITY",
            "ORPHANED_REFERENCE",
            "SHIFT_DISCREPANCY_MISMATCH");
  }

  @Test
  void reportsZeroFindingsWhenDataBuiltThroughRealServicesOnly() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantReconClean");
    actingAsUser(tenant.owner());

    // San pham bat dau tu TON = 0, nhap kho qua PurchaseOrderService that (ghi dung ca
    // inventory.stock LAN inventory_transactions tuong ung - khac han createProductWithStock()
    // insert thang stock khong co lich su).
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(50_000),
            BigDecimal.ZERO,
            BigDecimal.ZERO);
    var supplier = testDataFactory.createSupplier(tenant.tenant());
    PurchaseOrderRequest poRequest = new PurchaseOrderRequest();
    poRequest.setSupplierId(supplier.getId());
    poRequest.setBranchId(tenant.branchA().getId());
    PurchaseOrderItemRequest poItem = new PurchaseOrderItemRequest();
    poItem.setProductId(product.getId());
    poItem.setQuantity(BigDecimal.TEN);
    poItem.setUnitPrice(BigDecimal.valueOf(30_000));
    poRequest.setItems(List.of(poItem));
    poRequest.setPaidAmount(BigDecimal.valueOf(300_000));
    purchaseOrderService.create(poRequest);

    // Mo ca that, ban 1 don tien mat qua OrderService that (ghi dung order_items/invoice/
    // order_payments/inventory_transactions dong bo), roi dong ca that - moi so lieu deu do chinh
    // Service tinh ra, KHONG insert tay.
    OpenShiftRequest openRequest = new OpenShiftRequest();
    openRequest.setOpeningCash(BigDecimal.valueOf(200_000));
    ShiftDetailResponse shift = shiftService.open(openRequest);

    OrderCreateRequest orderRequest = new OrderCreateRequest();
    orderRequest.setBranchId(tenant.branchA().getId());
    orderRequest.setShiftId(shift.getId());
    OrderLineRequest line = new OrderLineRequest();
    line.setProductId(product.getId());
    line.setQuantity(BigDecimal.valueOf(2));
    line.setLineDiscountAmount(BigDecimal.ZERO);
    orderRequest.setLines(List.of(line));
    orderRequest.setExpectedTotalAmount(BigDecimal.valueOf(100_000));
    OrderPaymentRequest payment = new OrderPaymentRequest();
    payment.setMethod("cash");
    payment.setAmount(BigDecimal.valueOf(100_000));
    orderRequest.setPayments(List.of(payment));
    orderService.createOrder(orderRequest, "idem-recon-clean-" + System.nanoTime());

    CloseShiftRequest closeRequest = new CloseShiftRequest();
    // 200.000 dau ca + 100.000 ban tien mat = 300.000 dung khop, khong lech.
    closeRequest.setActualCash(BigDecimal.valueOf(300_000));
    shiftService.close(shift.getId(), closeRequest);

    ReconciliationRunResponse result =
        reconciliationService.runForTenant(
            tenant.tenant().getId(), ReconciliationRun.TRIGGER_MANUAL, null);

    assertThat(result.getStatus()).isEqualTo(ReconciliationRun.STATUS_COMPLETED);
    assertThat(result.getFindings()).isEmpty();
  }
}
