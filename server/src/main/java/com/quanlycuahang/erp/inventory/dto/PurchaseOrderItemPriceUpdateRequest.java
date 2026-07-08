package com.quanlycuahang.erp.inventory.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class PurchaseOrderItemPriceUpdateRequest {

  @NotNull @Positive private BigDecimal unitPrice;

  /** Bat buoc — ghi lai ly do sua (VD: "Nhap nham 15000 thanh 150000") de audit log the hien ro
   * tai sao gia von/cong no thay doi. */
  @NotBlank private String reason;

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
