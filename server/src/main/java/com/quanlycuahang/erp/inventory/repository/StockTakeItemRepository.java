package com.quanlycuahang.erp.inventory.repository;

import com.quanlycuahang.erp.inventory.entity.StockTakeItem;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface StockTakeItemRepository extends JpaRepository<StockTakeItem, Long> {

  /**
   * JOIN FETCH product — Prompt #7 (P2, hieu nang): truoc day derived query phang, moi noi goi ham
   * nay (StockTakeService.getById()/approve()) deu doc item.getProduct().getName() trong vong lap,
   * gay N+1 THAT SU (default_batch_fetch_size=50 giam con ~N/50 truy van thay vi N, van khong on) —
   * voi 1 phieu kiem ke toan bo kho (co the toi 20k SKU o tenant lon nhat theo muc tieu tai cua
   * roadmap), 1 lan xem chi tiet phieu se ban ra hang tram truy van rieng le. Phat hien khi do
   * baseline bang Hibernate Statistics (xem docs/PROJECT_STATE.md muc Prompt #7).
   */
  @Query("SELECT i FROM StockTakeItem i JOIN FETCH i.product WHERE i.stockTake.id = :stockTakeId")
  List<StockTakeItem> findByStockTakeId(
      @org.springframework.data.repository.query.Param("stockTakeId") Long stockTakeId);
}
