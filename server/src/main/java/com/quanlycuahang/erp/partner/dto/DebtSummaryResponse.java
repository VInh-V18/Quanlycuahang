package com.quanlycuahang.erp.partner.dto;

import java.math.BigDecimal;

public class DebtSummaryResponse {

  private BigDecimal receivableTotal;
  private long receivableCount;
  private BigDecimal payableTotal;
  private long payableCount;

  /** Tong no qua han (>30 ngay) chieu phai thu khach hang — "can thu" (FH-12). */
  private BigDecimal overdueReceivable;

  public BigDecimal getReceivableTotal() {
    return receivableTotal;
  }

  public void setReceivableTotal(BigDecimal receivableTotal) {
    this.receivableTotal = receivableTotal;
  }

  public long getReceivableCount() {
    return receivableCount;
  }

  public void setReceivableCount(long receivableCount) {
    this.receivableCount = receivableCount;
  }

  public BigDecimal getPayableTotal() {
    return payableTotal;
  }

  public void setPayableTotal(BigDecimal payableTotal) {
    this.payableTotal = payableTotal;
  }

  public long getPayableCount() {
    return payableCount;
  }

  public void setPayableCount(long payableCount) {
    this.payableCount = payableCount;
  }

  public BigDecimal getOverdueReceivable() {
    return overdueReceivable;
  }

  public void setOverdueReceivable(BigDecimal overdueReceivable) {
    this.overdueReceivable = overdueReceivable;
  }
}
