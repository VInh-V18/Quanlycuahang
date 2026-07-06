package com.quanlycuahang.erp.system.repository;

import com.quanlycuahang.erp.system.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {}
