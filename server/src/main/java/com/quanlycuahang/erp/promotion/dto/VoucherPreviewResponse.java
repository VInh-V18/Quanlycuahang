package com.quanlycuahang.erp.promotion.dto;

import java.math.BigDecimal;

public class VoucherPreviewResponse {

  private String code;
  private BigDecimal discountAmount;

  public VoucherPreviewResponse() {}

  public VoucherPreviewResponse(String code, BigDecimal discountAmount) {
    this.code = code;
    this.discountAmount = discountAmount;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(BigDecimal discountAmount) {
    this.discountAmount = discountAmount;
  }
}
