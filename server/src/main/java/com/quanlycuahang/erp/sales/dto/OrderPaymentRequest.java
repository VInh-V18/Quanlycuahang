package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class OrderPaymentRequest {

  /**
   * Rang buoc tap gia tri (khong con chuoi tu do) - doi soat tien mat cuoi ca (ShiftService) cong
   * theo khop CHINH XAC tung chuoi nay, 1 gia tri la (vd "Cash") truoc day van duoc nhan roi am
   * tham roi khoi bao cao doi soat (phat hien khi rieng soat).
   */
  @NotBlank
  @Pattern(
      regexp = "cash|bank_transfer|card",
      message = "Phương thức thanh toán phải là cash, bank_transfer hoặc card")
  private String method;

  @NotNull @Positive private BigDecimal amount;

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
