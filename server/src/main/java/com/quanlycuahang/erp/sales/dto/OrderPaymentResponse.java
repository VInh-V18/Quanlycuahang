package com.quanlycuahang.erp.sales.dto;

import java.math.BigDecimal;

public class OrderPaymentResponse {

  private String method;
  private BigDecimal amount;

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
