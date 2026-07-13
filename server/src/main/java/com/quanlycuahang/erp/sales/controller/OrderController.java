package com.quanlycuahang.erp.sales.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.report.excel.ReportExcelExporter;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderListItemResponse;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.service.OrderService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Idempotency-Key duoc xu ly boi {@link com.quanlycuahang.erp.sales.web.IdempotencyInterceptor}
 * truoc khi request toi day (B4 edge case 7) — Controller chi tap trung goi Service.
 */
@RestController
@RequestMapping("/api/v1/orders")
public class OrderController {

  private final OrderService orderService;
  private final ReportExcelExporter excelExporter;

  public OrderController(OrderService orderService, ReportExcelExporter excelExporter) {
    this.orderService = orderService;
    this.excelExporter = excelExporter;
  }

  @PostMapping
  @PreAuthorize("hasAuthority('order:create')")
  public ResponseEntity<ApiResponse<OrderResponse>> create(
      @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
      @Valid @RequestBody OrderCreateRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(orderService.createOrder(request, idempotencyKey)));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('order:view')")
  public ResponseEntity<ApiResponse<List<OrderListItemResponse>>> list(
      @RequestParam Long branchId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long cashierId,
      @RequestParam(required = false, defaultValue = "") String search,
      Pageable pageable) {
    return ResponseEntity.ok(
        orderService.list(branchId, from, to, status, cashierId, search, pageable));
  }

  @GetMapping("/export")
  @PreAuthorize("hasAuthority('order:view') and hasAuthority('report:export')")
  public ResponseEntity<byte[]> export(
      @RequestParam Long branchId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) Long cashierId,
      @RequestParam(required = false, defaultValue = "") String search) {
    List<OrderListItemResponse> data =
        orderService
            .list(
                branchId,
                from,
                to,
                status,
                cashierId,
                search,
                org.springframework.data.domain.PageRequest.of(0, 10_000))
            .getData();
    byte[] file =
        excelExporter.export(
            "Don hang",
            List.of("Mã đơn", "Thời gian", "Khách hàng", "Thu ngân", "Tổng tiền", "Trạng thái"),
            data,
            row ->
                new Object[] {
                  row.getOrderNumber(),
                  row.getCreatedAt(),
                  row.getCustomerName(),
                  row.getCashierName(),
                  row.getTotalAmount(),
                  row.getStatus()
                });
    return ResponseEntity.ok()
        .contentType(
            MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"don-hang.xlsx\"")
        .body(file);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('order:view')")
  public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(orderService.getById(id)));
  }
}
