package com.quanlycuahang.erp.sales.controller;

import com.quanlycuahang.erp.common.audit.Audited;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.exception.ValidationException;
import com.quanlycuahang.erp.report.excel.ReportExcelExporter;
import com.quanlycuahang.erp.sales.dto.EditOrderRequest;
import com.quanlycuahang.erp.sales.dto.OrderCreateRequest;
import com.quanlycuahang.erp.sales.dto.OrderListItemResponse;
import com.quanlycuahang.erp.sales.dto.OrderResponse;
import com.quanlycuahang.erp.sales.service.OrderEditService;
import com.quanlycuahang.erp.sales.service.OrderService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
  private final OrderEditService orderEditService;
  private final ReportExcelExporter excelExporter;

  public OrderController(
      OrderService orderService,
      OrderEditService orderEditService,
      ReportExcelExporter excelExporter) {
    this.orderService = orderService;
    this.orderEditService = orderEditService;
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
    ApiResponse<List<OrderListItemResponse>> page =
        orderService.list(
            branchId,
            from,
            to,
            status,
            cashierId,
            search,
            org.springframework.data.domain.PageRequest.of(0, 10_000));
    // Xuat toi da 10.000 dong/lan (SXSSFWorkbook streaming, khong phai gioi han RAM) — neu ket
    // qua thuc te vuot con so nay, file se bi CAT NGANG ma nguoi dung khong biet, tuong nham la
    // du lieu day (phat hien khi rieng soat). Bao loi ro rang thay vi xuat lang le thieu dong.
    if (page.getMeta().getTotal() > 10_000) {
      throw new ValidationException(
          "Có "
              + page.getMeta().getTotal()
              + " đơn hàng, vượt quá 10.000 dòng cho phép xuất 1 lần — vui lòng thu hẹp khoảng ngày/bộ lọc");
    }
    List<OrderListItemResponse> data = page.getData();
    byte[] file =
        excelExporter.export(
            "Don hang",
            List.of(
                "Mã đơn",
                "Thời gian",
                "Khách hàng",
                "SĐT",
                "Thu ngân",
                "Tổng tiền",
                "Thanh toán",
                "Trạng thái"),
            data,
            row ->
                new Object[] {
                  row.getOrderNumber(),
                  row.getCreatedAt(),
                  row.getCustomerName(),
                  row.getCustomerPhone(),
                  row.getCashierName(),
                  row.getTotalAmount(),
                  row.getPaymentLabel(),
                  row.getStatusLabel()
                });
    return excelExporter.toXlsxResponse(file, "don-hang.xlsx");
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('order:view')")
  public ResponseEntity<ApiResponse<OrderResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(orderService.getById(id)));
  }

  @PostMapping("/{id}/cancel")
  @PreAuthorize("hasAuthority('order:void')")
  @Audited(action = "ORDER_CANCEL", entityName = "Order")
  public ResponseEntity<ApiResponse<OrderResponse>> cancel(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(id)));
  }

  @PutMapping("/{id}")
  @PreAuthorize("hasAuthority('order:edit')")
  @Audited(action = "ORDER_EDIT", entityName = "Order")
  public ResponseEntity<ApiResponse<OrderResponse>> edit(
      @PathVariable Long id, @Valid @RequestBody EditOrderRequest request) {
    orderEditService.editOrder(id, request);
    return ResponseEntity.ok(ApiResponse.success(orderService.getById(id)));
  }
}
