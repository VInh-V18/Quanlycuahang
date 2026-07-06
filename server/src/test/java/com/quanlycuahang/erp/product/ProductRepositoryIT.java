package com.quanlycuahang.erp.product;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.product.repository.ProductRepository;
import java.math.BigDecimal;
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
 * Integration test Phase 3 Gate: Entity load duoc qua Testcontainers + Flyway migrate that tren
 * PostgreSQL that (khong H2/mock). Ghi chu: moi truong sandbox lam Phase nay KHONG co Docker daemon
 * — da xac minh thay the bang cach chay that Spring Boot + PostgreSQL 16 local (khong qua
 * Testcontainers) va quan sat Flyway + Hibernate ddl-auto=validate pass (xem docs/phase3/erd.md).
 * File nay van la code chay duoc dung tren moi truong CI/local co Docker.
 */
@Testcontainers
@SpringBootTest
class ProductRepositoryIT {

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

  @Autowired private ProductRepository productRepository;

  @Test
  void savesAndLoadsProductThroughFlywayMigratedSchema() {
    Product product = new Product();
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
}
