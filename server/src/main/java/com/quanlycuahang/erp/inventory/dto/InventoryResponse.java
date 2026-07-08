package com.quanlycuahang.erp.inventory.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class InventoryResponse {

  private Long id;
  private Long productId;
  private String productName;
  private String sku;
  private BigDecimal stock;
  private BigDecimal costPrice;
  private BigDecimal minStock;

  /** Lo/HSD gan nhat con hieu luc (FH-4) — null neu san pham chua tung nhap kem lo/HSD. */
  private String nearestBatchCode;

  private LocalDate nearestExpiryDate;

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

  public String getSku() {
    return sku;
  }

  public void setSku(String sku) {
    this.sku = sku;
  }

  public BigDecimal getStock() {
    return stock;
  }

  public void setStock(BigDecimal stock) {
    this.stock = stock;
  }

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
  }

  public BigDecimal getMinStock() {
    return minStock;
  }

  public void setMinStock(BigDecimal minStock) {
    this.minStock = minStock;
  }

  public String getNearestBatchCode() {
    return nearestBatchCode;
  }

  public void setNearestBatchCode(String nearestBatchCode) {
    this.nearestBatchCode = nearestBatchCode;
  }

  public LocalDate getNearestExpiryDate() {
    return nearestExpiryDate;
  }

  public void setNearestExpiryDate(LocalDate nearestExpiryDate) {
    this.nearestExpiryDate = nearestExpiryDate;
  }
}
