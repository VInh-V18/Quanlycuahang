package com.quanlycuahang.erp.operation.repository;

import com.quanlycuahang.erp.operation.entity.Invoice;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InvoiceRepository extends JpaRepository<Invoice, Long> {

  Optional<Invoice> findByOrderId(Long orderId);

  Optional<Invoice> findByLookupCode(String lookupCode);

  /**
   * Danh sach hoa don co loc, dung cho trang Hoa don — cung tieu chi loc nhu danh sach Don hang
   * (FH-9) nhung sap xep/tim theo hoa don (issued_at, invoice_number).
   */
  @Query(
      value =
          "SELECT i.id, i.invoice_number, i.issued_at, o.order_number, o.total_amount, "
              + "COALESCE(c.name, 'Khách lẻ') AS customer_name, c.phone AS customer_phone "
              + "FROM invoices i JOIN orders o ON o.id = i.order_id "
              + "LEFT JOIN customers c ON c.id = o.customer_id "
              + "WHERE o.branch_id = :branchId AND i.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR i.issued_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR i.issued_at < CAST(:to AS timestamptz)) "
              + "AND (:search = '' OR i.invoice_number ILIKE '%' || :search || '%' "
              + "     OR o.order_number ILIKE '%' || :search || '%' "
              + "     OR c.name ILIKE '%' || :search || '%' OR c.phone ILIKE '%' || :search || '%') "
              + "ORDER BY i.issued_at DESC",
      countQuery =
          "SELECT count(*) FROM invoices i JOIN orders o ON o.id = i.order_id "
              + "LEFT JOIN customers c ON c.id = o.customer_id "
              + "WHERE o.branch_id = :branchId AND i.tenant_id = :tenantId "
              + "AND (CAST(:from AS timestamptz) IS NULL OR i.issued_at >= CAST(:from AS timestamptz)) "
              + "AND (CAST(:to AS timestamptz) IS NULL OR i.issued_at < CAST(:to AS timestamptz)) "
              + "AND (:search = '' OR i.invoice_number ILIKE '%' || :search || '%' "
              + "     OR o.order_number ILIKE '%' || :search || '%' "
              + "     OR c.name ILIKE '%' || :search || '%' OR c.phone ILIKE '%' || :search || '%')",
      nativeQuery = true)
  Page<Object[]> search(
      @Param("branchId") Long branchId,
      @Param("from") OffsetDateTime from,
      @Param("to") OffsetDateTime to,
      @Param("search") String search,
      @Param("tenantId") Long tenantId,
      Pageable pageable);
}
