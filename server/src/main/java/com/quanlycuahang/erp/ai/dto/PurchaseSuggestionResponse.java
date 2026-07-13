package com.quanlycuahang.erp.ai.dto;

import java.math.BigDecimal;

/**
 * 1 dong goi y nhap hang - {@code confidence} la null cho "Gợi ý từ AI" (Prompt #11, thuat toan xac
 * dinh, khong co khai niem do tin cay) va co gia tri cho "Gợi ý AI nâng cao" (Prompt #12, qua
 * Python ML service - IsolationForest tra ve diem tin cay dua tren do day du/on dinh cua du lieu
 * ban 90 ngay, xem AiForecastService).
 */
public class PurchaseSuggestionResponse {

  private Long productId;
  private String productName;
  private String sku;
  private BigDecimal currentStock;
  private BigDecimal minStock;
  private BigDecimal suggestedQty;
  private BigDecimal confidence;

  public PurchaseSuggestionResponse(
      Long productId,
      String productName,
      String sku,
      BigDecimal currentStock,
      BigDecimal minStock,
      BigDecimal suggestedQty) {
    this(productId, productName, sku, currentStock, minStock, suggestedQty, null);
  }

  public PurchaseSuggestionResponse(
      Long productId,
      String productName,
      String sku,
      BigDecimal currentStock,
      BigDecimal minStock,
      BigDecimal suggestedQty,
      BigDecimal confidence) {
    this.productId = productId;
    this.productName = productName;
    this.sku = sku;
    this.currentStock = currentStock;
    this.minStock = minStock;
    this.suggestedQty = suggestedQty;
    this.confidence = confidence;
  }

  public Long getProductId() {
    return productId;
  }

  public String getProductName() {
    return productName;
  }

  public String getSku() {
    return sku;
  }

  public BigDecimal getCurrentStock() {
    return currentStock;
  }

  public BigDecimal getMinStock() {
    return minStock;
  }

  public BigDecimal getSuggestedQty() {
    return suggestedQty;
  }

  public BigDecimal getConfidence() {
    return confidence;
  }
}
