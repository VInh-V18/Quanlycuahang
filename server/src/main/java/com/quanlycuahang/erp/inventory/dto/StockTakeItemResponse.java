package com.quanlycuahang.erp.inventory.dto;

import java.math.BigDecimal;

public class StockTakeItemResponse {

  private Long id;
  private Long productId;
  private String productName;
  private BigDecimal expectedQty;
  private BigDecimal actualQty;
  private String reason;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public BigDecimal getExpectedQty() {
    return expectedQty;
  }

  public void setExpectedQty(BigDecimal expectedQty) {
    this.expectedQty = expectedQty;
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
