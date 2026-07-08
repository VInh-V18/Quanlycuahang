package com.quanlycuahang.erp.sales.pricing;

import java.math.BigDecimal;

public final class OrderLineResult {

  private final Long productId;
  private final BigDecimal unitPrice;
  private final BigDecimal quantity;
  private final BigDecimal discountAmount;
  private final BigDecimal vatAmount;
  private final BigDecimal lineTotal;

  public OrderLineResult(
      Long productId,
      BigDecimal unitPrice,
      BigDecimal quantity,
      BigDecimal discountAmount,
      BigDecimal vatAmount,
      BigDecimal lineTotal) {
    this.productId = productId;
    this.unitPrice = unitPrice;
    this.quantity = quantity;
    this.discountAmount = discountAmount;
    this.vatAmount = vatAmount;
    this.lineTotal = lineTotal;
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

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public BigDecimal getLineTotal() {
    return lineTotal;
  }
}
