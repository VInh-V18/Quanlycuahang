package com.quanlycuahang.erp.operation.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.operation.dto.InvoiceDetailResponse;
import com.quanlycuahang.erp.operation.dto.InvoiceListItemResponse;
import com.quanlycuahang.erp.operation.service.InvoiceDetailService;
import java.time.LocalDate;
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
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

  private final InvoiceDetailService invoiceDetailService;

  public InvoiceController(InvoiceDetailService invoiceDetailService) {
    this.invoiceDetailService = invoiceDetailService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('invoice:view')")
  public ResponseEntity<ApiResponse<List<InvoiceListItemResponse>>> list(
      @RequestParam Long branchId,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to,
      @RequestParam(required = false, defaultValue = "") String search,
      Pageable pageable) {
    return ResponseEntity.ok(invoiceDetailService.list(branchId, from, to, search, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('invoice:view')")
  public ResponseEntity<ApiResponse<InvoiceDetailResponse>> getById(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(invoiceDetailService.getById(id)));
  }

  /**
   * Tra cuu cong khai qua QR tren hoa don giay (khach hang khong can dang nhap) — chi lo dung
   * lookupCode (UUID rut gon ngau nhien), khong cho phep duyet/liet ke hoa don khac (IDOR an toan
   * vi khong doan duoc lookupCode tu id tuan tu).
   */
  @GetMapping("/lookup/{lookupCode}")
  public ResponseEntity<ApiResponse<InvoiceDetailResponse>> getByLookupCode(
      @PathVariable String lookupCode) {
    return ResponseEntity.ok(ApiResponse.success(invoiceDetailService.getByLookupCode(lookupCode)));
  }
}
