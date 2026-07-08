package com.quanlycuahang.erp.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Bat Spring Data JPA Auditing de tu dong dien createdAt/updatedAt tren BaseEntity. */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {}
