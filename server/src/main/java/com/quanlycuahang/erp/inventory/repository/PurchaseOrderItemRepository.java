package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.PurchaseOrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

  /**
   * JOIN FETCH product — cung N+1 nhu StockTakeItemRepository: PurchaseOrderMapper.toItemResponse
   * doc product.name cho tung dong, PurchaseOrderService.getById() lap qua toan bo danh sach.
   */
  @Query(
      "SELECT i FROM PurchaseOrderItem i JOIN FETCH i.product WHERE i.purchaseOrder.id ="
          + " :purchaseOrderId")
  List<PurchaseOrderItem> findByPurchaseOrderId(@Param("purchaseOrderId") Long purchaseOrderId);
}
