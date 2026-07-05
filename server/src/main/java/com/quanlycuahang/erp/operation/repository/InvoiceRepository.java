package com.quanlycuahang.erp.operation.repository;

import com.quanlycuahang.erp.operation.entity.Invoice;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  Optional<Invoice> findByOrderId(Long orderId);
}
