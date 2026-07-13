package com.quanlycuahang.erp.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Phase 11 Gate: khoa lai bug that tim thay o FH-12 — findAgingByPartner JOIN
 * UNION ALL giua customers/suppliers theo id trung lap giua 2 bang (khach hang #1 va NCC #1 cung co
 * id=1) tung khien ca 2 ten doi tac lan vao chung 1 chieu cong no truoc khi sua bang cot "kind".
 * Tao tenant/customer/supplier MOI qua TestDataFactory (thay vi dua vao V2__seed_data.sql) de tu
 * dam bao id trung nhau giua 2 bang bat ke thu tu chay test - dong thoi actingAsTenant() de dung
 * dung tenant_id that thay vi hardcode "1L" nhu ban cu (truoc Prompt #1 khong bind TenantContext).
 */
class DebtRepositoryIT extends AbstractIntegrationTest {

  @Autowired private DebtRepository debtRepository;
  @Autowired private TestDataFactory testDataFactory;

  @Test
  void agingByPartnerNeverMixesCustomerAndSupplierNamesAcrossDirections() {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantDebt");
    actingAsTenant(tenant.tenant().getId());

    Customer customer =
        testDataFactory.createCustomerWithDebtLimit(tenant.tenant(), BigDecimal.valueOf(5_000_000));
    Supplier supplier = testDataFactory.createSupplier(tenant.tenant());

    Debt receivable = new Debt();
    receivable.setTenant(tenant.tenant());
    receivable.setCustomer(customer);
    receivable.setDirection("receivable");
    receivable.setAmount(BigDecimal.valueOf(500_000));
    receivable.setOriginalAmount(BigDecimal.valueOf(500_000));
    debtRepository.save(receivable);

    Debt payable = new Debt();
    payable.setTenant(tenant.tenant());
    payable.setSupplier(supplier);
    payable.setDirection("payable");
    payable.setAmount(BigDecimal.valueOf(700_000));
    payable.setOriginalAmount(BigDecimal.valueOf(700_000));
    debtRepository.save(payable);

    List<Object[]> receivableAging =
        debtRepository.findAgingByPartner("receivable", tenant.tenant().getId());
    List<Object[]> payableAging =
        debtRepository.findAgingByPartner("payable", tenant.tenant().getId());

    // Chieu receivable chi duoc chua ten khach hang, KHONG duoc lan ten nha cung cap dù trung id.
    assertThat(receivableAging)
        .extracting(row -> (String) row[1])
        .contains(customer.getName())
        .doesNotContain(supplier.getName());

    // Chieu payable chi duoc chua ten nha cung cap, KHONG duoc lan ten khach hang dù trung id.
    assertThat(payableAging)
        .extracting(row -> (String) row[1])
        .contains(supplier.getName())
        .doesNotContain(customer.getName());
  }
}
