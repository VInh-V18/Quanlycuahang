package com.quanlycuahang.erp.inventory.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class InventoryCostPriceUpdateRequest {

  @NotNull @PositiveOrZero private BigDecimal costPrice;

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
  }
}
