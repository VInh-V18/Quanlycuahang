package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.StockTakeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockTakeItemRepository extends JpaRepository<StockTakeItem, Long> {

  List<StockTakeItem> findByStockTakeId(Long stockTakeId);
}
