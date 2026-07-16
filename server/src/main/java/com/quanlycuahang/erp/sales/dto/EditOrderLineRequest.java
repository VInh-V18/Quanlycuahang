package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/**
 * Khac OrderLineRequest (dung khi tao don): co them unitPrice tuong minh — luc tao don, don gia
 * LUON lay tu product.getSellPrice() song (Backend khong tin FE), nhung Sua don la thao tac
 * owner/manager ghi de gia thu cong len 1 don da chot, nen can nhan gia truc tiep tu request.
 */
public class EditOrderLineRequest {

  @NotNull private Long productId;

  @NotNull @Positive private BigDecimal quantity;

  @NotNull @PositiveOrZero private BigDecimal unitPrice;

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

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }

  public BigDecimal getLineDiscountAmount() {
    return lineDiscountAmount;
  }

  public void setLineDiscountAmount(BigDecimal lineDiscountAmount) {
    this.lineDiscountAmount = lineDiscountAmount;
  }
}
