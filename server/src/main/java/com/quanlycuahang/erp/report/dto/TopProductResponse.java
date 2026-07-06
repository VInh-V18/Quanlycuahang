package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

public class TopProductResponse {

  private Long productId;
  private String productName;
  private String sku;
  private BigDecimal quantitySold;
  private BigDecimal revenue;

  public TopProductResponse() {}

  public TopProductResponse(
      Long productId, String productName, String sku, BigDecimal quantitySold, BigDecimal revenue) {
    this.productId = productId;
    this.productName = productName;
    this.sku = sku;
    this.quantitySold = quantitySold;
    this.revenue = revenue;
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

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public BigDecimal getQuantitySold() {
    return quantitySold;
  }

  public void setQuantitySold(BigDecimal quantitySold) {
    this.quantitySold = quantitySold;
  }

  public BigDecimal getRevenue() {
    return revenue;
  }

  public void setRevenue(BigDecimal revenue) {
    this.revenue = revenue;
  }
}
