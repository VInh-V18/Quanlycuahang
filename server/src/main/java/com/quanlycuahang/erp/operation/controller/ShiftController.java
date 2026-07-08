package com.quanlycuahang.erp.operation.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.operation.dto.CashTransactionRequest;
import com.quanlycuahang.erp.operation.dto.CashTransactionResponse;
import com.quanlycuahang.erp.operation.dto.CloseShiftRequest;
import com.quanlycuahang.erp.operation.dto.OpenShiftRequest;
import com.quanlycuahang.erp.operation.dto.ShiftDetailResponse;
import com.quanlycuahang.erp.operation.dto.ShiftSummaryResponse;
import com.quanlycuahang.erp.operation.service.ShiftService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Ca & ket tien — mo/dong ca, thu chi tien mat, lich su ca (FH-14). */
@RestController
@RequestMapping("/api/v1/shifts")
public class ShiftController {

  private final ShiftService shiftService;

  public ShiftController(ShiftService shiftService) {
    this.shiftService = shiftService;
  }

  @PostMapping("/open")
  @PreAuthorize("hasAuthority('shift:open')")
  public ResponseEntity<ApiResponse<ShiftDetailResponse>> open(
      @Valid @RequestBody OpenShiftRequest request) {
    return ResponseEntity.ok(ApiResponse.success(shiftService.open(request)));
  }

  @GetMapping("/current")
  @PreAuthorize("hasAuthority('shift:view')")
  public ResponseEntity<ApiResponse<ShiftDetailResponse>> current() {
    return ResponseEntity.ok(ApiResponse.success(shiftService.getCurrent()));
  }

  @PostMapping("/{id}/close")
  @PreAuthorize("hasAuthority('shift:close')")
  public ResponseEntity<ApiResponse<ShiftDetailResponse>> close(
      @PathVariable Long id, @Valid @RequestBody CloseShiftRequest request) {
    return ResponseEntity.ok(ApiResponse.success(shiftService.close(id, request)));
  }

  @GetMapping
  @PreAuthorize("hasAuthority('shift:view')")
  public ResponseEntity<ApiResponse<List<ShiftSummaryResponse>>> history(
      @RequestParam(required = false) String status, Pageable pageable) {
    return ResponseEntity.ok(shiftService.history(status, pageable));
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('shift:view')")
  public ResponseEntity<ApiResponse<ShiftDetailResponse>> detail(@PathVariable Long id) {
    return ResponseEntity.ok(ApiResponse.success(shiftService.getDetail(id)));
  }

  @PostMapping("/{id}/cash-transactions")
  @PreAuthorize("hasAuthority('cash-transaction:create')")
  public ResponseEntity<ApiResponse<CashTransactionResponse>> addCashTransaction(
      @PathVariable Long id, @Valid @RequestBody CashTransactionRequest request) {
    return ResponseEntity.ok(ApiResponse.success(shiftService.addCashTransaction(id, request)));
  }
}
