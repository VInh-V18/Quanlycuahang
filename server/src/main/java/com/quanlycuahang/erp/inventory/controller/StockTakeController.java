package com.quanlycuahang.erp.inventory.controller;

import com.quanlycuahang.erp.common.audit.Audited;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.inventory.dto.StockTakeCreateRequest;
import com.quanlycuahang.erp.inventory.dto.StockTakeResponse;
import com.quanlycuahang.erp.inventory.dto.StockTakeSubmitRequest;
import com.quanlycuahang.erp.inventory.service.StockTakeService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/stock-takes")
public class StockTakeController {

  private final StockTakeService stockTakeService;

  public StockTakeController(StockTakeService stockTakeService) {
    this.stockTakeService = stockTakeService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('stock-take:view')")
  public ResponseEntity<ApiResponse<List<StockTakeResponse>>> list(
      @RequestParam Long branchId, Pageable pageable) {
    return ResponseEntity.ok(stockTakeService.list(branchId, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('stock-take:view')")
  public ResponseEntity<ApiResponse<StockTakeResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(stockTakeService.getById(id)));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('stock-take:create')")
  public ResponseEntity<ApiResponse<StockTakeResponse>> create(
      @Valid @RequestBody StockTakeCreateRequest request) {
    return ResponseEntity.ok(ApiResponse.success(stockTakeService.create(request)));
  }

  @PutMapping("/{id}/counts")
  @PreAuthorize("hasAuthority('stock-take:create')")
  public ResponseEntity<ApiResponse<StockTakeResponse>> submitCounts(
      @PathVariable Long id, @Valid @RequestBody StockTakeSubmitRequest request) {
    return ResponseEntity.ok(ApiResponse.success(stockTakeService.submitCounts(id, request)));
  }

  @PostMapping("/{id}/approve")
  @PreAuthorize("hasAuthority('stock-take:approve')")
  // Duyet kiem ke sua truc tiep ton kho (anh huong tai chinh) - truoc day khong ghi audit log
  // trong khi cac hanh dong tuong duong (sua gia nhap, xoa san pham) deu co (phat hien khi rieng
  // soat).
  @Audited(action = "STOCK_TAKE_APPROVE", entityName = "StockTake")
  public ResponseEntity<ApiResponse<StockTakeResponse>> approve(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(stockTakeService.approve(id)));
  }
}
