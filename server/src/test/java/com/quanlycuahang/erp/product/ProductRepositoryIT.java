package com.quanlycuahang.erp.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Integration test Phase 3 Gate: Entity load duoc qua Testcontainers/Postgres that + Flyway migrate
 * that, VOI Hibernate @Filter tenant BAT giong production (qua
 * AbstractIntegrationTest.actingAsTenant()) - truoc Prompt #1, test nay KHONG bind TenantContext du
 * cot tenant_id la NOT NULL, nen khong thuc su xac nhan hanh vi @Filter giong production.
 */
class ProductRepositoryIT extends AbstractIntegrationTest {

  @Autowired private ProductRepository productRepository;
  @Autowired private TestDataFactory testDataFactory;

  @Test
  void savesAndLoadsProductThroughFlywayMigratedSchema() {
    TestDataFactory.TestTenant tenantA = testDataFactory.createTenantWithBranches("tenantA");
    actingAsTenant(tenantA.tenant().getId());

    Product product = new Product();
    product.setTenant(tenantA.tenant());
    product.setSku("TEST-000001");
    product.setName("San pham test");
    product.setUnit("Cai");
    product.setSellPrice(BigDecimal.valueOf(10000));
    product.setVatRate(BigDecimal.TEN);
    product.setMinStock(BigDecimal.ZERO);

    Product saved = productRepository.save(product);

    assertThat(productRepository.findById(saved.getId())).isPresent();
    assertThat(productRepository.existsBySku("TEST-000001")).isTrue();
  }

  @Test
  void tenantFilterHidesProductsBelongingToOtherTenant() {
    TestDataFactory.TestTenant tenantA = testDataFactory.createTenantWithBranches("tenantA");
    TestDataFactory.TestTenant tenantB = testDataFactory.createTenantWithBranches("tenantB");

    Product productOfB =
        testDataFactory.createProductWithStock(
            tenantB.tenant(),
            tenantB.branchA(),
            BigDecimal.valueOf(20000),
            BigDecimal.valueOf(10000),
            BigDecimal.TEN);

    actingAsTenant(tenantA.tenant().getId());

    assertThat(productRepository.findById(productOfB.getId()))
        .as("Hibernate @Filter phai an san pham cua tenant khac ngay ca khi biet dung ID")
        .isEmpty();
  }
}
