package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class OrderLineRequest {

  @NotNull private Long productId;

  @NotNull @Positive private BigDecimal quantity;

  @NotNull @PositiveOrZero private BigDecimal lineDiscountAmount = BigDecimal.ZERO;

  public Long getProductId() {
    return productId;
  }

  public void setProductId(Long productId) {
    this.productId = productId;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getLineDiscountAmount() {
    return lineDiscountAmount;
  }

  public void setLineDiscountAmount(BigDecimal lineDiscountAmount) {
    this.lineDiscountAmount = lineDiscountAmount;
  }
}
