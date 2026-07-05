package com.quanlycuahang.erp.sales.pricing;

import java.math.BigDecimal;
import java.util.List;

public final class OrderPricingRequest {

  private final List<OrderLineInput> lines;
  private final BigDecimal orderDiscountAmount;
  private final BigDecimal voucherAmount;
  private final boolean priceIncludesVat;
  private final BigDecimal roundingUnit;
  private final BigDecimal cashReceived;

  public OrderPricingRequest(
      List<OrderLineInput> lines,
      BigDecimal orderDiscountAmount,
      BigDecimal voucherAmount,
      boolean priceIncludesVat,
      BigDecimal roundingUnit,
      BigDecimal cashReceived) {
    this.lines = lines;
    this.orderDiscountAmount = orderDiscountAmount == null ? BigDecimal.ZERO : orderDiscountAmount;
    this.voucherAmount = voucherAmount == null ? BigDecimal.ZERO : voucherAmount;
    this.priceIncludesVat = priceIncludesVat;
    this.roundingUnit = roundingUnit == null ? BigDecimal.ONE : roundingUnit;
    this.cashReceived = cashReceived;
  }

  public List<OrderLineInput> getLines() {
    return lines;
  }

  public BigDecimal getOrderDiscountAmount() {
    return orderDiscountAmount;
  }

  public BigDecimal getVoucherAmount() {
    return voucherAmount;
  }

  public boolean isPriceIncludesVat() {
    return priceIncludesVat;
  }

  public BigDecimal getRoundingUnit() {
    return roundingUnit;
  }

  public BigDecimal getCashReceived() {
    return cashReceived;
  }
}
