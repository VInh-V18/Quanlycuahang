package com.quanlycuahang.erp.product.repository;

import com.quanlycuahang.erp.product.entity.PriceHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

  Page<PriceHistory> findByProductIdOrderByCreatedAtDesc(Long productId, Pageable pageable);
}
