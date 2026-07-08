package com.quanlycuahang.erp.partner.repository;

import com.quanlycuahang.erp.partner.entity.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CustomerRepository extends JpaRepository<Customer, Long> {

  boolean existsByPhone(String phone);

  boolean existsByCustomerGroupId(Long customerGroupId);

  @Query(
      value =
          "SELECT * FROM customers c WHERE c.deleted_at IS NULL AND c.tenant_id = :tenantId AND (:search = '' "
              + "OR immutable_unaccent(lower(c.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "OR c.phone ILIKE '%' || :search || '%') ORDER BY c.name",
      countQuery =
          "SELECT count(*) FROM customers c WHERE c.deleted_at IS NULL AND c.tenant_id = :tenantId AND (:search = '' "
              + "OR immutable_unaccent(lower(c.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "OR c.phone ILIKE '%' || :search || '%')",
      nativeQuery = true)
  Page<Customer> search(
      @Param("search") String search, @Param("tenantId") Long tenantId, Pageable pageable);

  /**
   * Danh sach khach hang kem thong ke mua hang + cong no con du (FH-11) — dung cho trang Khach hang
   * (khac voi search() o tren, chi tra Customer tho dung cho POS quick-search).
   */
  @Query(
      value =
          "SELECT * FROM ("
              + "SELECT c.id AS id, c.name AS name, c.phone AS phone, c.address AS address, "
              + "c.customer_group_id AS customer_group_id, cg.name AS group_name, c.debt_limit AS debt_limit, "
              + "COALESCE(SUM(o.total_amount), 0) AS total_purchased, COUNT(o.id) AS order_count, "
              + "MAX(o.created_at) AS last_purchase_at, "
              + "COALESCE((SELECT SUM(d.amount) FROM debts d WHERE d.customer_id = c.id "
              + "AND d.direction = 'receivable' AND d.amount > 0 AND d.deleted_at IS NULL), 0) AS current_debt "
              + "FROM customers c "
              + "LEFT JOIN customer_groups cg ON cg.id = c.customer_group_id "
              + "LEFT JOIN orders o ON o.customer_id = c.id AND o.deleted_at IS NULL "
              + "AND o.status IN "
              + "('completed','partially_returned','fully_returned') "
              + "WHERE c.deleted_at IS NULL AND c.tenant_id = :tenantId "
              + "AND (:search = '' OR immutable_unaccent(lower(c.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "     OR c.phone ILIKE '%' || :search || '%') "
              + "AND (CAST(:customerGroupId AS bigint) IS NULL OR c.customer_group_id = CAST(:customerGroupId AS bigint)) "
              + "GROUP BY c.id, c.name, c.phone, c.address, c.customer_group_id, cg.name, c.debt_limit"
              + ") x "
              + "WHERE (CAST(:hasDebt AS boolean) IS NULL "
              + "       OR (CAST(:hasDebt AS boolean) = true AND x.current_debt > 0) "
              + "       OR (CAST(:hasDebt AS boolean) = false AND x.current_debt = 0)) "
              + "ORDER BY x.name",
      countQuery =
          "SELECT count(*) FROM ("
              + "SELECT c.id, "
              + "COALESCE((SELECT SUM(d.amount) FROM debts d WHERE d.customer_id = c.id "
              + "AND d.direction = 'receivable' AND d.amount > 0 AND d.deleted_at IS NULL), 0) AS current_debt "
              + "FROM customers c "
              + "WHERE c.deleted_at IS NULL AND c.tenant_id = :tenantId "
              + "AND (:search = '' OR immutable_unaccent(lower(c.name)) ILIKE '%' || immutable_unaccent(lower(:search)) || '%' "
              + "     OR c.phone ILIKE '%' || :search || '%') "
              + "AND (CAST(:customerGroupId AS bigint) IS NULL OR c.customer_group_id = CAST(:customerGroupId AS bigint))"
              + ") x "
              + "WHERE (CAST(:hasDebt AS boolean) IS NULL "
              + "       OR (CAST(:hasDebt AS boolean) = true AND x.current_debt > 0) "
              + "       OR (CAST(:hasDebt AS boolean) = false AND x.current_debt = 0))",
      nativeQuery = true)
  Page<Object[]> searchWithStats(
      @Param("search") String search,
      @Param("customerGroupId") Long customerGroupId,
      @Param("hasDebt") Boolean hasDebt,
      @Param("tenantId") Long tenantId,
      Pageable pageable);
}
