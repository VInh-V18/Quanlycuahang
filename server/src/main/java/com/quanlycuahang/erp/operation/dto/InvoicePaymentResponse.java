package com.quanlycuahang.erp.operation.dto;

import java.math.BigDecimal;

public class InvoicePaymentResponse {

  private String method;
  private BigDecimal amount;

  public InvoicePaymentResponse() {}

  public InvoicePaymentResponse(String method, BigDecimal amount) {
    this.method = method;
    this.amount = amount;
  }

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
