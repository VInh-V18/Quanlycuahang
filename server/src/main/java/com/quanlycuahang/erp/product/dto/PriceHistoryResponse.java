package com.quanlycuahang.erp.product.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class PriceHistoryResponse {

  private Long id;
  private BigDecimal oldPrice;
  private BigDecimal newPrice;
  private String changedByName;
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public BigDecimal getOldPrice() {
    return oldPrice;
  }

  public void setOldPrice(BigDecimal oldPrice) {
    this.oldPrice = oldPrice;
  }

  public BigDecimal getNewPrice() {
    return newPrice;
  }

  public void setNewPrice(BigDecimal newPrice) {
    this.newPrice = newPrice;
  }

  public String getChangedByName() {
    return changedByName;
  }

  public void setChangedByName(String changedByName) {
    this.changedByName = changedByName;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
