package com.quanlycuahang.erp.partner.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.math.BigDecimal;

public class DebtPaymentRequest {

  /** "receivable" (thu no khach hang) hoac "payable" (tra no NCC). */
  @NotBlank private String direction;

  @NotNull private Long partnerId;

  @NotNull @Positive private BigDecimal amount;

  @NotBlank private String method;

  private String note;

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public Long getPartnerId() {
    return partnerId;
  }

  public void setPartnerId(Long partnerId) {
    this.partnerId = partnerId;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
