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

  /**
   * stock * costPrice — tinh 1 lan o Service, dung chung cho man hinh Ton kho lan xuat Excel (truoc
   * day FE va Excel export moi noi tu nhan lai rieng, phat hien khi rieng soat).
   */
  private BigDecimal stockValue;

  /**
   * "near_expiry" | "low_stock" | "ok" — tinh 1 lan o InventoryService.computeStatus(), dung chung
   * cho man hinh Ton kho lan xuat Excel (truoc day moi noi tu tinh lai rieng bang nguong 7 ngay
   * hardcode doc lap, co the lech nhau neu sua 1 cho quen cho kia — phat hien khi rieng soat).
   */
  private String status;

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

  public BigDecimal getStockValue() {
    return stockValue;
  }

  public void setStockValue(BigDecimal stockValue) {
    this.stockValue = stockValue;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
