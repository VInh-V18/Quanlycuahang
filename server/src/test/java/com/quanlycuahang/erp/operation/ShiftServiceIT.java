package com.quanlycuahang.erp.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.auth.entity.Role;
import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.RoleRepository;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.exception.PermissionDeniedException;
import com.quanlycuahang.erp.operation.dto.CashTransactionRequest;
import com.quanlycuahang.erp.operation.dto.CloseShiftRequest;
import com.quanlycuahang.erp.operation.dto.OpenShiftRequest;
import com.quanlycuahang.erp.operation.dto.ShiftDetailResponse;
import com.quanlycuahang.erp.operation.service.ShiftService;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderLineRequest;
import com.quanlycuahang.erp.sales.dto.OrderPaymentRequest;
import com.quanlycuahang.erp.sales.service.OrderService;
import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Prompt #1 (P0): ShiftService - chan mo ca trung, tinh dung tien mat du kien khi
 * dong ca (ban tien mat + thu/chi - hoan tien mat), va chan IDOR (requireShift()) giua 2 nhan vien
 * khac nhau cung tenant.
 */
class ShiftServiceIT extends AbstractIntegrationTest {

  @Autowired private ShiftService shiftService;
  @Autowired private OrderService orderService;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private UserRepository userRepository;
  @Autowired private RoleRepository roleRepository;

  private User createSecondCashier(TestDataFactory.TestTenant tenant) {
    Role cashierRole = roleRepository.findByCode("cashier").orElseThrow();
    String suffix = UUID.randomUUID().toString().substring(0, 8);
    User user = new User();
    user.setTenant(tenant.tenant());
    user.setUsername("cashier2-" + suffix);
    user.setPasswordHash("{noop}unused");
    user.setFullName("Thu ngan test 2");
    user.setActive(true);
    user.setRoles(Set.of(cashierRole));
    user.setBranches(Set.of(tenant.branchA()));
    return userRepository.save(user);
  }

  @Test
  void openBlocksSecondShiftWhileFirstStillOpenForSameUser() {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantShiftOpen");
    actingAsUser(tenant.cashier());

    OpenShiftRequest request = new OpenShiftRequest();
    request.setOpeningCash(BigDecimal.valueOf(500_000));
    shiftService.open(request);

    OpenShiftRequest second = new OpenShiftRequest();
    second.setOpeningCash(BigDecimal.valueOf(300_000));
    assertThatThrownBy(() -> shiftService.open(second))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("chưa đóng");
  }

  @Test
  void closeComputesExpectedCashFromSalesCashInAndCashOut() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantShiftClose");
    actingAsUser(tenant.cashier());
    Product product =
        testDataFactory.createProductWithStock(
            tenant.tenant(),
            tenant.branchA(),
            BigDecimal.valueOf(100_000),
            BigDecimal.valueOf(60_000),
            BigDecimal.TEN);

    OpenShiftRequest openRequest = new OpenShiftRequest();
    openRequest.setOpeningCash(BigDecimal.valueOf(500_000));
    ShiftDetailResponse shift = shiftService.open(openRequest);

    OrderCreateRequest orderRequest = new OrderCreateRequest();
    orderRequest.setBranchId(tenant.branchA().getId());
    orderRequest.setShiftId(shift.getId());
    OrderLineRequest line = new OrderLineRequest();
    line.setProductId(product.getId());
    line.setQuantity(BigDecimal.ONE);
    line.setLineDiscountAmount(BigDecimal.ZERO);
    orderRequest.setLines(List.of(line));
    orderRequest.setExpectedTotalAmount(BigDecimal.valueOf(100_000));
    OrderPaymentRequest payment = new OrderPaymentRequest();
    payment.setMethod("cash");
    payment.setAmount(BigDecimal.valueOf(100_000));
    orderRequest.setPayments(List.of(payment));
    orderService.createOrder(orderRequest, "idem-shift-order-" + System.nanoTime());

    CashTransactionRequest cashIn = new CashTransactionRequest();
    cashIn.setType("cash_in");
    cashIn.setAmount(BigDecimal.valueOf(50_000));
    shiftService.addCashTransaction(shift.getId(), cashIn);

    CashTransactionRequest cashOut = new CashTransactionRequest();
    cashOut.setType("cash_out");
    cashOut.setAmount(BigDecimal.valueOf(20_000));
    shiftService.addCashTransaction(shift.getId(), cashOut);

    // Tien mat du kien = 500.000 (dau ca) + 100.000 (ban tien mat) - 0 (hoan tra) + 50.000 (thu) -
    // 20.000 (chi) = 630.000.
    CloseShiftRequest closeRequest = new CloseShiftRequest();
    closeRequest.setActualCash(BigDecimal.valueOf(630_000));
    ShiftDetailResponse closed = shiftService.close(shift.getId(), closeRequest);

    assertThat(closed.getExpectedCash()).isEqualByComparingTo(BigDecimal.valueOf(630_000));
    assertThat(closed.getDiscrepancy()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(closed.getCashSalesTotal()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
    assertThat(closed.getCashInTotal()).isEqualByComparingTo(BigDecimal.valueOf(50_000));
    assertThat(closed.getCashOutTotal()).isEqualByComparingTo(BigDecimal.valueOf(20_000));
    assertThat(closed.getStatus()).isEqualTo("closed");
  }

  @Test
  void closeRecordsDiscrepancyWhenActualCashDoesNotMatchExpected() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantShiftDiscrepancy");
    actingAsUser(tenant.cashier());

    OpenShiftRequest openRequest = new OpenShiftRequest();
    openRequest.setOpeningCash(BigDecimal.valueOf(500_000));
    ShiftDetailResponse shift = shiftService.open(openRequest);

    CloseShiftRequest closeRequest = new CloseShiftRequest();
    // Thuc te kiem duoc it hon 10.000 so voi ky vong (500.000).
    closeRequest.setActualCash(BigDecimal.valueOf(490_000));
    ShiftDetailResponse closed = shiftService.close(shift.getId(), closeRequest);

    assertThat(closed.getDiscrepancy()).isEqualByComparingTo(BigDecimal.valueOf(-10_000));
  }

  @Test
  void closeBlocksClosingAnAlreadyClosedShift() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantShiftReclose");
    actingAsUser(tenant.cashier());

    OpenShiftRequest openRequest = new OpenShiftRequest();
    openRequest.setOpeningCash(BigDecimal.valueOf(500_000));
    ShiftDetailResponse shift = shiftService.open(openRequest);

    CloseShiftRequest closeRequest = new CloseShiftRequest();
    closeRequest.setActualCash(BigDecimal.valueOf(500_000));
    shiftService.close(shift.getId(), closeRequest);

    CloseShiftRequest secondClose = new CloseShiftRequest();
    secondClose.setActualCash(BigDecimal.valueOf(500_000));
    assertThatThrownBy(() -> shiftService.close(shift.getId(), secondClose))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("đã được đóng");
  }

  @Test
  void requireShiftBlocksNonFullAccessUserFromTouchingAnotherCashiersShiftButOwnerCan() {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantShiftIdor");
    actingAsUser(tenant.cashier());
    OpenShiftRequest openRequest = new OpenShiftRequest();
    openRequest.setOpeningCash(BigDecimal.valueOf(200_000));
    ShiftDetailResponse shiftOpenedByCashier1 = shiftService.open(openRequest);

    User cashier2 = createSecondCashier(tenant);
    // Phai unbind EntityManager/TenantContext cua cashier1 truoc khi actingAsUser(cashier2) - 1
    // thread chi duoc bind 1 EntityManager tai 1 thoi diem (xem AbstractIntegrationTest), khac voi
    // cac test khac trong file nay chi actingAsUser() DUY NHAT 1 lan.
    unbindCurrentThread();
    actingAsUser(cashier2);
    assertThatThrownBy(() -> shiftService.getDetail(shiftOpenedByCashier1.getId()))
        .isInstanceOf(PermissionDeniedException.class);

    CloseShiftRequest closeRequest = new CloseShiftRequest();
    closeRequest.setActualCash(BigDecimal.valueOf(200_000));
    assertThatThrownBy(() -> shiftService.close(shiftOpenedByCashier1.getId(), closeRequest))
        .isInstanceOf(PermissionDeniedException.class);

    // owner co full access -> KHONG bi chan, doc/dong duoc ca cua cashier1 binh thuong.
    unbindCurrentThread();
    actingAsUser(tenant.owner());
    ShiftDetailResponse detail = shiftService.getDetail(shiftOpenedByCashier1.getId());
    assertThat(detail.getId()).isEqualTo(shiftOpenedByCashier1.getId());
  }
}
