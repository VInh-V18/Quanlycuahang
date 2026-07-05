package com.quanlycuahang.erp.inventory.service;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryTransactionResponse;
import com.quanlycuahang.erp.inventory.entity.Inventory;
import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import com.quanlycuahang.erp.inventory.mapper.InventoryMapper;
import com.quanlycuahang.erp.inventory.repository.InventoryRepository;
import com.quanlycuahang.erp.inventory.repository.InventoryTransactionRepository;
import java.util.List;
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
  private final InventoryMapper inventoryMapper;

  public InventoryService(
      InventoryRepository inventoryRepository,
      InventoryTransactionRepository inventoryTransactionRepository,
      InventoryMapper inventoryMapper) {
    this.inventoryRepository = inventoryRepository;
    this.inventoryTransactionRepository = inventoryTransactionRepository;
    this.inventoryMapper = inventoryMapper;
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryResponse>> listByBranch(Long branchId, Pageable pageable) {
    Page<Inventory> page = inventoryRepository.findByBranchId(branchId, pageable);
    return ApiResponse.page(page.map(inventoryMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryResponse>> lowStockByBranch(Long branchId, Pageable pageable) {
    Page<Inventory> page = inventoryRepository.findLowStockByBranchId(branchId, pageable);
    return ApiResponse.page(page.map(inventoryMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public ApiResponse<List<InventoryTransactionResponse>> transactionHistory(
      Long productId, Pageable pageable) {
    Page<InventoryTransaction> page =
        inventoryTransactionRepository.findByProductIdOrderByCreatedAtDesc(productId, pageable);
    return ApiResponse.page(page.map(inventoryMapper::toTransactionResponse));
  }
}
