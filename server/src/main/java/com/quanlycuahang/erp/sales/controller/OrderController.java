package com.quanlycuahang.erp.sales.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Idempotency-Key duoc xu ly boi {@link com.quanlycuahang.erp.sales.web.IdempotencyInterceptor}
 * truoc khi request toi day (B4 edge case 7) — Controller chi tap trung goi Service.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderService orderService;

  public OrderController(OrderService orderService) {
    this.orderService = orderService;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('order:create')")
  public ResponseEntity<ApiResponse<OrderResponse>> create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody OrderCreateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(orderService.createOrder(request, idempotencyKey)));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('order:view')")
  public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(orderService.getById(id)));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('order:void')")
  public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(id)));
  }
}
