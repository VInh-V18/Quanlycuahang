package com.quanlycuahang.erp.inventory.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryResponse;
import com.quanlycuahang.erp.inventory.dto.InventoryTransactionResponse;
import com.quanlycuahang.erp.inventory.service.InventoryService;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/inventory")
public class InventoryController {

  private final InventoryService inventoryService;

  public InventoryController(InventoryService inventoryService) {
    this.inventoryService = inventoryService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryResponse>>> listByBranch(
      @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(inventoryService.listByBranch(branchId, pageable));
  }

  @GetMapping("/low-stock")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryResponse>>> lowStock(
      @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(inventoryService.lowStockByBranch(branchId, pageable));
  }

  @GetMapping("/products/{productId}/transactions")
  @PreAuthorize("hasAuthority('inventory:view')")
  public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> transactionHistory(
      @PathVariable Long productId, Pageable pageable) {
    return ResponseEntity.ok(inventoryService.transactionHistory(productId, pageable));
  }
}
