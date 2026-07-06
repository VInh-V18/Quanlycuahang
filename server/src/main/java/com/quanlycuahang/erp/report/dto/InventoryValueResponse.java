package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

/** label la ten chi nhanh hoac ten danh muc tuy groupBy. */
public class InventoryValueResponse {

  private String label;
  private BigDecimal totalValue;
  private BigDecimal totalQuantity;

  public InventoryValueResponse() {}

  public InventoryValueResponse(String label, BigDecimal totalValue, BigDecimal totalQuantity) {
    this.label = label;
    this.totalValue = totalValue;
    this.totalQuantity = totalQuantity;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public BigDecimal getTotalValue() {
    return totalValue;
  }

  public void setTotalValue(BigDecimal totalValue) {
    this.totalValue = totalValue;
  }

  public BigDecimal getTotalQuantity() {
    return totalQuantity;
  }

  public void setTotalQuantity(BigDecimal totalQuantity) {
    this.totalQuantity = totalQuantity;
  }
}
