package com.quanlycuahang.erp.promotion.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.promotion.dto.VoucherPreviewResponse;
import com.quanlycuahang.erp.promotion.service.VoucherService;
import com.quanlycuahang.erp.promotion.service.VoucherValidationResult;
import java.math.BigDecimal;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Xem truoc voucher tai POS (UC-11) — dung chung 1 VoucherService.validate() voi luc tao don that
 * (OrderService), tranh sai lech giua "xem truoc" va "ap dung that".
 */
@RestController
@RequestMapping("/api/v1/vouchers")
public class VoucherController {

  private final VoucherService voucherService;

  public VoucherController(VoucherService voucherService) {
    this.voucherService = voucherService;
  }

  @GetMapping("/validate")
  @PreAuthorize("hasAuthority('order:create')")
  public ResponseEntity<ApiResponse<VoucherPreviewResponse>> validate(
      @RequestParam String code, @RequestParam BigDecimal subtotal) {
    VoucherValidationResult result = voucherService.validate(code, subtotal);
    return ResponseEntity.ok(
        ApiResponse.success(
            new VoucherPreviewResponse(result.voucher().getCode(), result.discountAmount())));
  }
}
