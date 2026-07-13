package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.Inventory;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

  Optional<Inventory> findByProductIdAndBranchId(Long productId, Long branchId);

  /** Khoa pessimistic khi can tranh race condition luc nhap/ban dong thoi cung 1 dong ton kho. */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId AND i.branch.id = :branchId")
  Optional<Inventory> findByProductIdAndBranchIdForUpdate(
      @Param("productId") Long productId, @Param("branchId") Long branchId);

  /**
   * Khoi tao dong ton kho = 0 neu CHUA ton tai (ON CONFLICT DO NOTHING), dung ngay truoc khi khoa
   * lai bang findByProductIdAndBranchIdForUpdate() - SELECT...FOR UPDATE chi khoa duoc dong DA ton
   * tai, nen voi san pham LAN DAU nhap tai 1 chi nhanh, 2 phieu nhap chay dong thoi truoc day cung
   * thay "chua co" roi cung INSERT, va vao uq_inventory_product_branch va nem loi tho 500 (phat
   * hien khi rieng soat). Voi ON CONFLICT DO NOTHING, ben den sau tu dong cho ben dau commit roi bo
   * qua, sau do SELECT...FOR UPDATE lai la thay va khoa duoc dong do nhu binh thuong. tenant_id
   * phai truyen tuong minh (native query khong qua @Filter/@PrePersist).
   */
  @org.springframework.data.jpa.repository.Modifying
  @Query(
      value =
          "INSERT INTO inventory (tenant_id, product_id, branch_id, stock, cost_price, version) "
              + "VALUES (:tenantId, :productId, :branchId, 0, 0, 0) "
              + "ON CONFLICT (product_id, branch_id) DO NOTHING",
      nativeQuery = true)
  void initializeIfAbsent(
      @Param("tenantId") Long tenantId,
      @Param("productId") Long productId,
      @Param("branchId") Long branchId);

  /**
   * JOIN FETCH product — InventoryMapper.toResponse() doc product.name/sku/minStock cho tung dong;
   * danh sach ton kho la trang duoc xem nhieu nhat va co the toi hang chuc nghin dong/chi nhanh,
   * khong JOIN FETCH se ban N truy van rieng le (giam con ~N/50 nho default_batch_fetch_size nhung
   * van khong on) - N+1 THAT SU, phat hien khi do baseline Prompt #7.
   */
  @Query("SELECT i FROM Inventory i JOIN FETCH i.product WHERE i.branch.id = :branchId")
  Page<Inventory> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);

  java.util.List<Inventory> findByBranchId(Long branchId);

  List<Inventory> findByBranchIdAndProductIdIn(Long branchId, List<Long> productIds);

  @Query(
      "SELECT i FROM Inventory i JOIN FETCH i.product p WHERE i.branch.id = :branchId AND"
          + " i.stock <= p.minStock")
  Page<Inventory> findLowStockByBranchId(@Param("branchId") Long branchId, Pageable pageable);

  /**
   * Tim theo ten/SKU + loc can HSD NGAY TAI SERVER (JPQL nen van di qua Hibernate @Filter tenant
   * nhu binh thuong, khong can tu tay them tenant_id nhu native query) - truoc day FE loc bang
   * useMemo tren du lieu CUA 1 TRANG DA FETCH, nen san pham chi nam o trang khac se bao "khong tim
   * thay" du thuc su ton tai (phat hien khi rieng soat).
   */
  @Query(
      "SELECT i FROM Inventory i JOIN FETCH i.product p WHERE i.branch.id = :branchId "
          + "AND (:search = '' OR LOWER(p.name) LIKE CONCAT('%', :search, '%') "
          + "     OR LOWER(p.sku) LIKE CONCAT('%', :search, '%')) "
          + "AND (:expiryThreshold IS NULL OR EXISTS ("
          + "  SELECT 1 FROM InventoryBatch b WHERE b.product.id = p.id "
          + "  AND b.branch.id = i.branch.id AND b.expiryDate IS NOT NULL "
          + "  AND b.expiryDate <= :expiryThreshold))")
  Page<Inventory> search(
      @Param("branchId") Long branchId,
      @Param("search") String search,
      @Param("expiryThreshold") java.time.LocalDate expiryThreshold,
      Pageable pageable);

  /** Nhu search() o tren nhung chi lay dong duoi dinh muc (Chi hang duoi dinh muc). */
  @Query(
      "SELECT i FROM Inventory i JOIN FETCH i.product p WHERE i.branch.id = :branchId AND"
          + " i.stock <= p.minStock "
          + "AND (:search = '' OR LOWER(p.name) LIKE CONCAT('%', :search, '%') "
          + "     OR LOWER(p.sku) LIKE CONCAT('%', :search, '%')) "
          + "AND (:expiryThreshold IS NULL OR EXISTS ("
          + "  SELECT 1 FROM InventoryBatch b WHERE b.product.id = p.id "
          + "  AND b.branch.id = i.branch.id AND b.expiryDate IS NOT NULL "
          + "  AND b.expiryDate <= :expiryThreshold))")
  Page<Inventory> searchLowStock(
      @Param("branchId") Long branchId,
      @Param("search") String search,
      @Param("expiryThreshold") java.time.LocalDate expiryThreshold,
      Pageable pageable);

  /**
   * Gia tri ton kho theo gia von (Phase 10) — nhom theo chi nhanh (:branchId = null xem tat ca).
   */
  @Query(
      value =
          "SELECT b.name AS label, COALESCE(SUM(i.stock * i.cost_price), 0) AS total_value, "
              + "COALESCE(SUM(i.stock), 0) AS total_quantity "
              + "FROM inventory i JOIN branches b ON b.id = i.branch_id "
              + "WHERE i.tenant_id = :tenantId AND (:branchId IS NULL OR i.branch_id = :branchId) "
              + "GROUP BY b.id, b.name ORDER BY total_value DESC",
      nativeQuery = true)
  List<Object[]> findInventoryValueByBranch(
      @Param("branchId") Long branchId, @Param("tenantId") Long tenantId);

  /** Gia tri ton kho theo gia von, nhom theo danh muc san pham trong 1 chi nhanh (Phase 10). */
  @Query(
      value =
          "SELECT c.name AS label, COALESCE(SUM(i.stock * i.cost_price), 0) AS total_value, "
              + "COALESCE(SUM(i.stock), 0) AS total_quantity "
              + "FROM inventory i "
              + "JOIN products p ON p.id = i.product_id "
              + "JOIN categories c ON c.id = p.category_id "
              + "WHERE i.tenant_id = :tenantId AND (:branchId IS NULL OR i.branch_id = :branchId) "
              + "GROUP BY c.id, c.name ORDER BY total_value DESC",
      nativeQuery = true)
  List<Object[]> findInventoryValueByCategory(
      @Param("branchId") Long branchId, @Param("tenantId") Long tenantId);
}
