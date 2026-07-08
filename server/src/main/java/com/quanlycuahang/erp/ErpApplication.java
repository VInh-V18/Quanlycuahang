package com.quanlycuahang.erp;

import com.quanlycuahang.erp.common.repository.TenantAwareRepositoryImpl;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * repositoryBaseClass = TenantAwareRepositoryImpl: thay the SimpleJpaRepository mac dinh cho toan
 * bo 34 JpaRepository (khong chi khai bao rieng tung cai) - va lo hong Hibernate @Filter khong loc
 * duoc cac truy van "theo ID truc tiep" (findById/existsById/findAllById/getReferenceById), xem
 * chi tiet o TenantAwareRepositoryImpl.
 */
@SpringBootApplication
@EnableJpaRepositories(
    basePackages = "com.quanlycuahang.erp",
    repositoryBaseClass = TenantAwareRepositoryImpl.class)
public class ErpApplication {

  public static void main(String[] args) {
    SpringApplication.run(ErpApplication.class, args);
  }
}
