package com.quanlycuahang.erp;

import com.quanlycuahang.erp.common.repository.TenantAwareRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * repositoryBaseClass = TenantAwareRepositoryImpl: thay the SimpleJpaRepository mac dinh cho toan
 * bo 34 JpaRepository (khong chi khai bao rieng tung cai) - va lo hong Hibernate @Filter khong loc
 * duoc cac truy van "theo ID truc tiep" (findById/existsById/findAllById/getReferenceById), xem chi
 * tiet o TenantAwareRepositoryImpl.
 *
 * <p>@EnableAsync: dung boi InvoiceEmailService de gui email hoa don CHAY NEN, khong chan request
 * checkout cho SMTP tra loi (phat hien khi rieng soat hieu nang) — cac tac vu @Async can tu bind
 * lai TenantContext/Hibernate filter qua TenantSessionBinder vi ThreadLocal khong tu ke thua sang
 * thread moi cua executor.
 *
 * <p>@EnableScheduling: dung boi ReconciliationScheduledJob (Prompt #6) chay doi soat toan ven du
 * lieu dinh ky cho moi tenant dang hoat dong.
 */
@SpringBootApplication
@EnableJpaRepositories(
    basePackages = "com.quanlycuahang.erp",
    repositoryBaseClass = TenantAwareRepositoryImpl.class)
@EnableAsync
@EnableScheduling
public class ErpApplication {

  public static void main(String[] args) {
    SpringApplication.run(ErpApplication.class, args);
  }
}
