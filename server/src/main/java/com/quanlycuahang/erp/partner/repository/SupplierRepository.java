package com.quanlycuahang.erp.partner.repository;

import com.quanlycuahang.erp.partner.entity.Supplier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

  /** Danh sach NCC kem thong ke nhap hang + cong no phai tra con du (FH-11). */
  @Query(
      value =
          "SELECT s.id AS id, s.name AS name, s.phone AS phone, s.address AS address, "
              + "COALESCE(SUM(po.total_amount), 0) AS total_purchased, COUNT(po.id) AS order_count, "
              + "MAX(po.created_at) AS last_purchase_at, "
              + "COALESCE((SELECT SUM(d.amount) FROM debts d WHERE d.supplier_id = s.id "
              + "AND d.direction = 'payable' AND d.amount > 0 AND d.deleted_at IS NULL), 0) AS current_debt "
              + "FROM suppliers s "
              + "LEFT JOIN purchase_orders po ON po.supplier_id = s.id AND po.deleted_at IS NULL "
              + "WHERE s.deleted_at IS NULL "
              + "AND (:search = '' OR immutable_unaccent(lower(s.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "     OR s.phone ILIKE '%' || :search || '%') "
              + "GROUP BY s.id, s.name, s.phone, s.address "
              + "ORDER BY s.name",
      countQuery =
          "SELECT count(*) FROM suppliers s WHERE s.deleted_at IS NULL "
              + "AND (:search = '' OR immutable_unaccent(lower(s.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "     OR s.phone ILIKE '%' || :search || '%')",
      nativeQuery = true)
  Page<Object[]> searchWithStats(@Param("search") String search, Pageable pageable);
}
