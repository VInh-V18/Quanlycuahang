package com.quanlycuahang.erp.sales.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.sales.dto.ParkedOrderRequest;
import com.quanlycuahang.erp.sales.dto.ParkedOrderResponse;
import com.quanlycuahang.erp.sales.service.ParkedOrderService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/parked-orders")
public class ParkedOrderController {

  private final ParkedOrderService parkedOrderService;

  public ParkedOrderController(ParkedOrderService parkedOrderService) {
    this.parkedOrderService = parkedOrderService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('order:park')")
  public ResponseEntity<ApiResponse<List<ParkedOrderResponse>>> list(@RequestParam Long branchId) {
    return ResponseEntity.ok(ApiResponse.success(parkedOrderService.listByBranch(branchId)));
  }

  @PostMapping
  @PreAuthorize("hasAuthority('order:park')")
  public ResponseEntity<ApiResponse<ParkedOrderResponse>> park(
      @Valid @RequestBody ParkedOrderRequest request) {
    return ResponseEntity.ok(ApiResponse.success(parkedOrderService.park(request)));
  }

  @PostMapping("/{id}/resume")
  @PreAuthorize("hasAuthority('order:park')")
  public ResponseEntity<ApiResponse<ParkedOrderResponse>> resume(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(parkedOrderService.resume(id)));
  }
}
