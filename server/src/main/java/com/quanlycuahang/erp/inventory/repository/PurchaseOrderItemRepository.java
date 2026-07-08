package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.PurchaseOrderItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderItemRepository extends JpaRepository<PurchaseOrderItem, Long> {

  List<PurchaseOrderItem> findByPurchaseOrderId(Long purchaseOrderId);
}
