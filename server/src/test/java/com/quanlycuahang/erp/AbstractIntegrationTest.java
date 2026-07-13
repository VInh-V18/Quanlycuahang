package com.quanlycuahang.erp;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.auth.security.TenantSessionBinder;
import jakarta.persistence.EntityManager;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Base class dung chung cho MOI integration test cham DB that qua Postgres/Redis - tu bat
 * TenantContext + Hibernate @Filter "tenantFilter" giong production (qua TenantSessionBinder, cung
 * co che TenantFilter dung cho request HTTP that) thay vi de moi test tu nho set tay.
 *
 * <p>3 IT test co san truoc Prompt #1 (ProductRepositoryIT, DebtRepositoryIT,
 * OrderRepositoryRevenueIT) KHONG lam viec nay du tenant_id la NOT NULL o moi bang nghiep vu -
 * nghia la cac test do dang dua vao 1 hanh vi KHONG giong production that (hoac dang fail ngam, xem
 * BUGS_FOUND.md). Test moi (va 3 test cu sau khi cap nhat) PHAI goi actingAsTenant(tenantId) truoc
 * khi thao tac repository/service, dung 1 lan bind() moi @Test giong 1 request that.
 *
 * <p>Quan ly vong doi container THU CONG (khong dung annotation @Testcontainers/@Container cua
 * JUnit) de ho tro ca 2 che do: (a) mac dinh, tu tao Postgres+Redis qua Testcontainers nhu chuan;
 * (b) tro vao Postgres/Redis NGOAI da co san qua bien moi truong IT_DB_HOST/IT_DB_PORT/IT_DB_NAME/
 * IT_DB_USER/IT_DB_PASSWORD/IT_REDIS_PORT - dung khi moi truong khong cho Testcontainers thuong
 * luong duoc voi Docker daemon qua Java (gap phai khi lam Prompt #1: Testcontainers 1.20.3 khong
 * ket noi duoc Docker Desktop tren may phat trien cu the nay qua named pipe, tra ve HTTP 400 rong o
 * /info du lenh `docker` CLI van hoat dong binh thuong). CI/may co Docker hoat dong binh thuong voi
 * Testcontainers thi KHONG can set cac bien nay.
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

  private static final String EXTERNAL_DB_HOST = System.getenv("IT_DB_HOST");
  private static final PostgreSQLContainer<?> POSTGRES;
  private static final GenericContainer<?> REDIS;

  static {
    if (EXTERNAL_DB_HOST == null) {
      POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");
      POSTGRES.start();
      REDIS = new GenericContainer<>("redis:7-alpine").withExposedPorts(6379);
      REDIS.start();
    } else {
      POSTGRES = null;
      REDIS = null;
    }
  }

  @DynamicPropertySource
  static void configureProperties(DynamicPropertyRegistry registry) {
    if (EXTERNAL_DB_HOST != null) {
      String dbPort = System.getenv().getOrDefault("IT_DB_PORT", "5432");
      String dbName = System.getenv().getOrDefault("IT_DB_NAME", "quanlycuahang_test");
      String dbUser = System.getenv().getOrDefault("IT_DB_USER", "postgres");
      String dbPassword = System.getenv().getOrDefault("IT_DB_PASSWORD", "testpass");
      String redisPort = System.getenv().getOrDefault("IT_REDIS_PORT", "6379");
      registry.add(
          "spring.datasource.url",
          () -> "jdbc:postgresql://" + EXTERNAL_DB_HOST + ":" + dbPort + "/" + dbName);
      registry.add("spring.datasource.username", () -> dbUser);
      registry.add("spring.datasource.password", () -> dbPassword);
      registry.add("spring.data.redis.host", () -> EXTERNAL_DB_HOST);
      registry.add("spring.data.redis.port", () -> redisPort);
      // Prompt #11 (AI Assistant) - pool doc rieng cua role fruithouse_ai_readonly (V30) PHAI tro
      // toi CUNG 1 database test nay, khong phai gia tri mac dinh DB_HOST/DB_PORT/DB_NAME (khac
      // bien IT_DB_* o tren) trong application.yml - neu khong, test se vo tinh noi toi 1
      // Postgres KHAC (vd may local port 5432 mac dinh), che mat ket qua that cua bai test quyen.
      registry.add(
          "app.ai.readonly-datasource.jdbc-url",
          () -> "jdbc:postgresql://" + EXTERNAL_DB_HOST + ":" + dbPort + "/" + dbName);
    } else {
      registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
      registry.add("spring.datasource.username", POSTGRES::getUsername);
      registry.add("spring.datasource.password", POSTGRES::getPassword);
      registry.add("spring.data.redis.host", REDIS::getHost);
      registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
      registry.add("app.ai.readonly-datasource.jdbc-url", POSTGRES::getJdbcUrl);
    }
  }

  @Autowired private TenantSessionBinder tenantSessionBinder;

  // ThreadLocal (khong phai field thuong) vi 1 so test (vd kiem tra oversell dong thoi) tu goi
  // actingAsTenant()/actingAsUser() tu NHIEU THREAD cua chinh no trong CUNG 1 @Test - 1 field
  // thuong se bi ghi de lan nhau giua cac thread, lam @AfterEach chi don dung 1 trong so do va
  // lo cac EntityManager con lai.
  private final ThreadLocal<EntityManager> boundEntityManager = new ThreadLocal<>();

  /**
   * Bat TenantContext + Hibernate @Filter cho dung tenantId truyen vao, y het 1 request that (xem
   * TenantFilter) - goi 1 lan dau moi @Test (hoac dau moi thread con tu tao) truoc khi thao tac
   * repository/service, KHONG boc @Transactional o test method (de dung dung ranh gioi transaction
   * that cua Service, khong phai transaction rollback gia lap cua Spring Test).
   */
  protected void actingAsTenant(Long tenantId) {
    TenantContext.set(tenantId);
    boundEntityManager.set(tenantSessionBinder.bind(tenantId));
  }

  /**
   * Nhu actingAsTenant() nhung dong thoi bat SecurityContextHolder voi username cua user truyen vao
   * - can cho CurrentUserProvider.getCurrentUser() (dung boi OrderService/ReturnService/... de ghi
   * nguoi thuc hien) hoat dong dung trong test, vi khong co request HTTP that di qua
   * JwtAuthenticationFilter.
   */
  protected void actingAsUser(User user) {
    actingAsTenant(user.getTenantId());
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(user.getUsername(), null, List.of()));
  }

  /**
   * Don TenantContext/EntityManager/SecurityContext cua THREAD HIEN TAI - bat buoc tu goi o cuoi
   * moi thread con tu tao trong test (vd kiem tra oversell dong thoi), vi @AfterEach cua JUnit chi
   * chay tren thread chinh, khong biet den cac thread do.
   */
  protected void unbindCurrentThread() {
    EntityManager em = boundEntityManager.get();
    if (em != null) {
      tenantSessionBinder.unbind(em);
      boundEntityManager.remove();
    }
    TenantContext.clear();
    SecurityContextHolder.clearContext();
  }

  @AfterEach
  void clearTenantContext() {
    unbindCurrentThread();
  }
}
