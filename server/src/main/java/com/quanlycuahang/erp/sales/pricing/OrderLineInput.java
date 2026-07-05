package com.quanlycuahang.erp.sales.pricing;

import java.math.BigDecimal;

/** 1 dong hang dau vao cho OrderPricingService — Java thuan, khong phu thuoc Entity/Spring. */
public final class OrderLineInput {

  private final Long productId;
  private final BigDecimal unitPrice;
  private final BigDecimal quantity;
  private final BigDecimal lineDiscountAmount;
  private final BigDecimal vatRate;

  public OrderLineInput(
      Long productId,
      BigDecimal unitPrice,
      BigDecimal quantity,
      BigDecimal lineDiscountAmount,
      BigDecimal vatRate) {
    this.productId = productId;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.lineDiscountAmount = lineDiscountAmount == null ? BigDecimal.ZERO : lineDiscountAmount;
    this.vatRate = vatRate == null ? BigDecimal.ZERO : vatRate;
  }

  public Long getProductId() {
    return productId;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public BigDecimal getLineDiscountAmount() {
    return lineDiscountAmount;
  }

  public BigDecimal getVatRate() {
    return vatRate;
  }
}
