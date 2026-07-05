package com.quanlycuahang.erp.sales.pricing;

import java.math.BigDecimal;
import java.util.List;

public final class OrderPricingResult {

  private final List<OrderLineResult> lines;
  private final BigDecimal subtotalAmount;
  private final BigDecimal discountAmount;
  private final BigDecimal vatAmount;
  private final BigDecimal roundingAdjustment;
  private final BigDecimal totalAmount;
  private final BigDecimal changeAmount;

  public OrderPricingResult(
      List<OrderLineResult> lines,
      BigDecimal subtotalAmount,
      BigDecimal discountAmount,
      BigDecimal vatAmount,
      BigDecimal roundingAdjustment,
      BigDecimal totalAmount,
      BigDecimal changeAmount) {
    this.lines = lines;
    this.subtotalAmount = subtotalAmount;
    this.discountAmount = discountAmount;
    this.vatAmount = vatAmount;
    this.roundingAdjustment = roundingAdjustment;
    this.totalAmount = totalAmount;
    this.changeAmount = changeAmount;
  }

  public List<OrderLineResult> getLines() {
    return lines;
  }

  public BigDecimal getSubtotalAmount() {
    return subtotalAmount;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public BigDecimal getRoundingAdjustment() {
    return roundingAdjustment;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public BigDecimal getChangeAmount() {
    return changeAmount;
  }
}
