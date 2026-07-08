package com.quanlycuahang.erp.partner;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.partner.entity.Debt;
import com.quanlycuahang.erp.partner.entity.Supplier;
import com.quanlycuahang.erp.partner.repository.CustomerRepository;
import com.quanlycuahang.erp.partner.repository.DebtRepository;
import com.quanlycuahang.erp.partner.repository.SupplierRepository;
import java.math.BigDecimal;
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
 * Integration test Phase 11 Gate: khoa lai bug that tim thay o FH-12 — findAgingByPartner JOIN
 * UNION ALL giua customers/suppliers theo id trung lap giua 2 bang (khach hang #1 va NCC #1 cung co
 * id=1) tung khien ca 2 ten doi tac lan vao chung 1 chieu cong no truoc khi sua bang cot "kind".
 * Chay tren PostgreSQL that qua Testcontainers (khong mock) vi loi nay chi lo ra qua JOIN SQL that,
 * unit test JPQL/mock khong the phat hien (dung nhu 2 IT truoc o Phase 3/5 da phat hien bug that
 * qua ha tang that). Ghi chu moi truong: sandbox lam Phase 11 nay KHONG co Docker daemon (da xac
 * minh qua `docker ps` that bai) — test da duoc verify gian tiep bang cach chay dung query nay tren
 * Postgres 16 local (khong qua Testcontainers) trong suot phien FH-12; file nay chay duoc that tren
 * moi truong CI/local co Docker.
 */
@Testcontainers
@SpringBootTest
class DebtRepositoryIT {

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

  @Autowired private CustomerRepository customerRepository;
  @Autowired private SupplierRepository supplierRepository;
  @Autowired private DebtRepository debtRepository;

  @Test
  void agingByPartnerNeverMixesCustomerAndSupplierNamesAcrossDirections() {
    // V2__seed_data.sql da tao san 5 khach hang + 3 nha cung cap -> id customer/supplier chac chan
    // trung nhau (ca 2 bang deu co id=1..3), dung dung tinh huong da gay bug o FH-12.
    Customer customer = customerRepository.findAll().get(0);
    Supplier supplier = supplierRepository.findAll().get(0);

    Debt receivable = new Debt();
    receivable.setCustomer(customer);
    receivable.setDirection("receivable");
    receivable.setAmount(BigDecimal.valueOf(500_000));
    receivable.setOriginalAmount(BigDecimal.valueOf(500_000));
    debtRepository.save(receivable);

    Debt payable = new Debt();
    payable.setSupplier(supplier);
    payable.setDirection("payable");
    payable.setAmount(BigDecimal.valueOf(700_000));
    payable.setOriginalAmount(BigDecimal.valueOf(700_000));
    debtRepository.save(payable);

    List<Object[]> receivableAging = debtRepository.findAgingByPartner("receivable", 1L);
    List<Object[]> payableAging = debtRepository.findAgingByPartner("payable", 1L);

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
