package com.quanlycuahang.erp.partner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.partner.dto.DebtPaymentRequest;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.repository.DebtPaymentRepository;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.partner.service.DebtService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Prompt #1 (P0): DebtService.recordPayment() - phan bo FIFO qua nhieu Debt con du
 * cua 1 doi tac theo createdAt ASC, va chan thanh toan vuot tong cong no con du (rollback toan bo
 * thay doi da ghi tam trong vong lap truoc khi nem loi, nho @Transactional).
 */
class DebtServiceIT extends AbstractIntegrationTest {

  @Autowired private DebtService debtService;
  @Autowired private TestDataFactory testDataFactory;
  @Autowired private DebtRepository debtRepository;
  @Autowired private DebtPaymentRepository debtPaymentRepository;

  private Debt receivableDebt(
      TestDataFactory.TestTenant tenant, Customer customer, BigDecimal amount) {
    Debt debt = new Debt();
    debt.setTenant(tenant.tenant());
    debt.setCustomer(customer);
    debt.setDirection("receivable");
    debt.setAmount(amount);
    debt.setOriginalAmount(amount);
    return debtRepository.save(debt);
  }

  @Test
  void paymentAllocatesFifoAcrossOutstandingDebtsOldestFirst() throws InterruptedException {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantDebtFifo");
    actingAsUser(tenant.owner());
    Customer customer =
        testDataFactory.createCustomerWithDebtLimit(
            tenant.tenant(), BigDecimal.valueOf(10_000_000));

    Debt older = receivableDebt(tenant, customer, BigDecimal.valueOf(300_000));
    // Dam bao created_at cua 2 khoan no khac nhau ro rang (Instant do @CreatedDate gan tu dong luc
    // persist, khong the set tay - xem BaseEntity) de ORDER BY createdAt ASC xac dinh dung thu tu.
    Thread.sleep(10);
    Debt newer = receivableDebt(tenant, customer, BigDecimal.valueOf(500_000));

    DebtPaymentRequest request = new DebtPaymentRequest();
    request.setDirection("receivable");
    request.setPartnerId(customer.getId());
    request.setAmount(BigDecimal.valueOf(400_000)); // vua het no cu (300k) + 1 phan no moi (100k)
    request.setMethod("cash");
    debtService.recordPayment(request);

    Debt reloadedOlder = debtRepository.findById(older.getId()).orElseThrow();
    Debt reloadedNewer = debtRepository.findById(newer.getId()).orElseThrow();
    assertThat(reloadedOlder.getAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(reloadedNewer.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(400_000));

    List<com.quanlycuahang.erp.partner.entity.DebtPayment> payments =
        debtPaymentRepository.findByDebtIdIn(List.of(older.getId(), newer.getId()));
    assertThat(payments).hasSize(2);
    BigDecimal totalPaid =
        payments.stream()
            .map(com.quanlycuahang.erp.partner.entity.DebtPayment::getAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(totalPaid).isEqualByComparingTo(BigDecimal.valueOf(400_000));
  }

  @Test
  void paymentExceedingTotalOutstandingIsBlockedAndRollsBackPartialAllocation() {
    TestDataFactory.TestTenant tenant =
        testDataFactory.createTenantWithBranches("tenantDebtExceed");
    actingAsUser(tenant.owner());
    Customer customer =
        testDataFactory.createCustomerWithDebtLimit(
            tenant.tenant(), BigDecimal.valueOf(10_000_000));
    Debt debt = receivableDebt(tenant, customer, BigDecimal.valueOf(100_000));

    DebtPaymentRequest request = new DebtPaymentRequest();
    request.setDirection("receivable");
    request.setPartnerId(customer.getId());
    request.setAmount(BigDecimal.valueOf(150_000)); // vuot 100.000 con no
    request.setMethod("cash");

    assertThatThrownBy(() -> debtService.recordPayment(request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("vượt quá");

    // Rollback toan bo: khoan no KHONG duoc giam mot phan nao, khong duoc ghi nhan DebtPayment
    // "mo coi" nao ca (transaction @Transactional cua recordPayment phai roll back het).
    Debt reloaded = debtRepository.findById(debt.getId()).orElseThrow();
    assertThat(reloaded.getAmount()).isEqualByComparingTo(BigDecimal.valueOf(100_000));
    assertThat(debtPaymentRepository.findByDebtIdIn(List.of(debt.getId()))).isEmpty();
  }

  @Test
  void paymentBlocksWhenPartnerHasNoOutstandingDebt() {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantDebtNone");
    actingAsUser(tenant.owner());
    Customer customer =
        testDataFactory.createCustomerWithDebtLimit(
            tenant.tenant(), BigDecimal.valueOf(10_000_000));

    DebtPaymentRequest request = new DebtPaymentRequest();
    request.setDirection("receivable");
    request.setPartnerId(customer.getId());
    request.setAmount(BigDecimal.valueOf(50_000));
    request.setMethod("cash");

    assertThatThrownBy(() -> debtService.recordPayment(request))
        .isInstanceOf(BusinessRuleException.class)
        .hasMessageContaining("không còn công nợ");
  }
}
