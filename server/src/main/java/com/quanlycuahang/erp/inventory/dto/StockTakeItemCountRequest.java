package com.quanlycuahang.erp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class StockTakeItemCountRequest {

  @NotNull private Long stockTakeItemId;

  @NotNull @PositiveOrZero private BigDecimal actualQty;

  private String reason;

  public Long getStockTakeItemId() {
    return stockTakeItemId;
  }

  public void setStockTakeItemId(Long stockTakeItemId) {
    this.stockTakeItemId = stockTakeItemId;
  }

  public BigDecimal getActualQty() {
    return actualQty;
  }

  public void setActualQty(BigDecimal actualQty) {
    this.actualQty = actualQty;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
