package com.quanlycuahang.erp.inventory.service;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryTransactionResponse;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.mapper.InventoryMapper;
import com.quanlycuahang.erp.inventory.repository.InventoryBatchRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Ton kho theo chi nhanh + the kho (UC-06). Ton hien tai luon tinh lai duoc tu InventoryTransaction
 * (B4).
 */
@Service
public class InventoryService {

  private final InventoryRepository inventoryRepository;
  private final InventoryTransactionRepository inventoryTransactionRepository;
  private final InventoryBatchRepository inventoryBatchRepository;
  private final InventoryMapper inventoryMapper;

  public InventoryService(
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      InventoryBatchRepository inventoryBatchRepository,
      InventoryMapper inventoryMapper) {
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.inventoryBatchRepository = inventoryBatchRepository;
    this.inventoryMapper = inventoryMapper;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryResponse>> listByBranch(Long branchId, Pageable pageable) {
    Page<Inventory> page = inventoryRepository.findByBranchId(branchId, pageable);
    ApiResponse<List<InventoryResponse>> response = ApiResponse.page(page.map(inventoryMapper::toResponse));
    enrichWithNearestBatch(response.getData(), branchId);
    return response;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryResponse>> lowStockByBranch(Long branchId, Pageable pageable) {
    Page<Inventory> page = inventoryRepository.findLowStockByBranchId(branchId, pageable);
    ApiResponse<List<InventoryResponse>> response = ApiResponse.page(page.map(inventoryMapper::toResponse));
    enrichWithNearestBatch(response.getData(), branchId);
    return response;
  }

  /** Gan them Lo/HSD gan nhat vao moi dong ton kho (FH-4/FH-7) — 1 truy van cho ca trang. */
  private void enrichWithNearestBatch(List<InventoryResponse> rows, Long branchId) {
    if (rows.isEmpty()) {
      return;
    }
    Map<Long, Object[]> byProductId = new HashMap<>();
    for (Object[] row : inventoryBatchRepository.findNearestBatchPerProduct(branchId)) {
      byProductId.put(((Number) row[0]).longValue(), row);
    }
    for (InventoryResponse row : rows) {
      Object[] batch = byProductId.get(row.getProductId());
      if (batch != null) {
        row.setNearestBatchCode((String) batch[1]);
        row.setNearestExpiryDate(toLocalDate(batch[2]));
      }
    }
  }

  private static LocalDate toLocalDate(Object value) {
    if (value == null) {
      return null;
    }
    if (value instanceof LocalDate localDate) {
      return localDate;
    }
    return ((java.sql.Date) value).toLocalDate();
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryTransactionResponse>> transactionHistory(
      Long productId, Pageable pageable) {
    Page<InventoryTransaction> page =
        inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    return ApiResponse.page(page.map(inventoryMapper::toTransactionResponse));
  }
}
