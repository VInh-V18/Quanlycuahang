package com.quanlycuahang.erp.partner.repository;

import com.quanlycuahang.erp.partner.entity.Debt;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DebtRepository extends JpaRepository<Debt, Long> {

  Page<Debt> findByCustomerId(Long customerId, Pageable pageable);

  Page<Debt> findBySupplierId(Long supplierId, Pageable pageable);

  java.util.List<Debt> findByReferenceTypeAndReferenceId(String referenceType, Long referenceId);
}
