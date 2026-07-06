package com.quanlycuahang.erp.partner.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.partner.dto.DebtHistoryEventResponse;
import com.quanlycuahang.erp.partner.dto.DebtPartnerAgingResponse;
import com.quanlycuahang.erp.partner.dto.DebtPaymentRequest;
import com.quanlycuahang.erp.partner.dto.DebtSummaryResponse;
import com.quanlycuahang.erp.partner.service.DebtService;
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

/**
 * Cong no chi tiet (FH-12) — khac voi ReportController.debtAging (Phase 10, tong hop toan he thong
 * theo 4 muc 0-30/31-60/61-90/90+); day la theo tung doi tac, 3 muc 0-7/8-30/>30, kem lich su doi
 * chieu + ghi nhan thanh toan.
 */
@RestController
@RequestMapping("/api/v1/debts")
public class DebtController {

  private final DebtService debtService;

  public DebtController(DebtService debtService) {
    this.debtService = debtService;
  }

  @GetMapping("/summary")
  @PreAuthorize("hasAuthority('debt:view')")
  public ResponseEntity<ApiResponse<DebtSummaryResponse>> summary() {
    return ResponseEntity.ok(ApiResponse.success(debtService.summary()));
  }

  @GetMapping("/by-partner")
  @PreAuthorize("hasAuthority('debt:view')")
  public ResponseEntity<ApiResponse<List<DebtPartnerAgingResponse>>> agingByPartner(
      @RequestParam String direction) {
    return ResponseEntity.ok(ApiResponse.success(debtService.agingByPartner(direction)));
  }

  @GetMapping("/partners/{partnerId}/history")
  @PreAuthorize("hasAuthority('debt:view')")
  public ResponseEntity<ApiResponse<List<DebtHistoryEventResponse>>> history(
      @PathVariable Long partnerId, @RequestParam String direction) {
    return ResponseEntity.ok(ApiResponse.success(debtService.history(direction, partnerId)));
  }

  @PostMapping("/payments")
  @PreAuthorize("hasAuthority('debt:collect-payment')")
  public ResponseEntity<ApiResponse<Void>> recordPayment(
      @Valid @RequestBody DebtPaymentRequest request) {
    debtService.recordPayment(request);
    return ResponseEntity.ok(ApiResponse.success(null));
  }
}
