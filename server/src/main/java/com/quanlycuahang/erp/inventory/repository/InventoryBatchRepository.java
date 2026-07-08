package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.InventoryBatch;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface InventoryBatchRepository extends JpaRepository<InventoryBatch, Long> {

  Optional<InventoryBatch> findByPurchaseOrderItemId(Long purchaseOrderItemId);

  /**
   * Lo gan het han nhat cho tung san pham trong 1 chi nhanh (DISTINCT ON theo product_id, sap xep
   * theo expiry_date tang dan — NULL xep cuoi) — dung cho cot "Lô/HSD" o Ton kho (FH-7) va canh bao
   * sap het han o Dashboard/Bao cao.
   */
  @Query(
      value =
          "SELECT DISTINCT ON (b.product_id) b.product_id, b.batch_code, b.expiry_date "
              + "FROM inventory_batches b "
              + "WHERE b.branch_id = :branchId AND b.tenant_id = :tenantId "
              + "ORDER BY b.product_id, b.expiry_date ASC NULLS LAST, b.received_at DESC",
      nativeQuery = true)
  List<Object[]> findNearestBatchPerProduct(
      @Param("branchId") Long branchId, @Param("tenantId") Long tenantId);

  /**
   * Danh sach lo sap het han trong N ngay toi (dung cho canh bao xa hang/chuong trinh giam gia).
   */
  @Query(
      value =
          "SELECT b.id, p.name, p.sku, b.batch_code, b.expiry_date, b.quantity "
              + "FROM inventory_batches b JOIN products p ON p.id = b.product_id "
              + "WHERE b.branch_id = :branchId AND b.tenant_id = :tenantId "
              + "AND b.expiry_date IS NOT NULL "
              + "AND b.expiry_date <= :threshold "
              + "ORDER BY b.expiry_date ASC",
      nativeQuery = true)
  List<Object[]> findExpiringSoon(
      @Param("branchId") Long branchId,
      @Param("threshold") LocalDate threshold,
      @Param("tenantId") Long tenantId);

  List<InventoryBatch> findByProductIdAndBranchIdOrderByExpiryDateAsc(
      Long productId, Long branchId);
}
