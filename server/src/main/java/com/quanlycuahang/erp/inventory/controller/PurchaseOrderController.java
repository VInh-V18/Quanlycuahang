package com.quanlycuahang.erp.inventory.controller;

import com.quanlycuahang.erp.common.audit.Audited;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderItemPriceUpdateRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderRequest;
import com.quanlycuahang.erp.inventory.dto.PurchaseOrderResponse;
import com.quanlycuahang.erp.inventory.service.PurchaseOrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/purchase-orders")
public class PurchaseOrderController {

  private final PurchaseOrderService purchaseOrderService;

  public PurchaseOrderController(PurchaseOrderService purchaseOrderService) {
    this.purchaseOrderService = purchaseOrderService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('purchase-order:view')")
  public ResponseEntity<ApiResponse<List<PurchaseOrderResponse>>> list(
      @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(purchaseOrderService.list(branchId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('purchase-order:view')")
  public ResponseEntity<ApiResponse<PurchaseOrderResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.getById(id)));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('purchase-order:create')")
  public ResponseEntity<ApiResponse<PurchaseOrderResponse>> create(
      @Valid @RequestBody PurchaseOrderRequest request) {
    return ResponseEntity.ok(ApiResponse.success(purchaseOrderService.create(request)));
  }

  @PatchMapping("/items/{itemId}/price")
  @PreAuthorize("hasAuthority('purchase-order:update')")
  @Audited(action = "PURCHASE_ORDER_ITEM_PRICE_UPDATE", entityName = "PurchaseOrderItem")
  public ResponseEntity<ApiResponse<PurchaseOrderResponse>> updateItemPrice(
      @PathVariable Long itemId, @Valid @RequestBody PurchaseOrderItemPriceUpdateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(purchaseOrderService.updateItemPrice(itemId, request)));
  }
}
