package com.quanlycuahang.erp.partner.dto;

import java.math.BigDecimal;

public class SupplierResponse {

  private Long id;
  private String name;
  private String phone;
  private String address;

  /** Tong cong no phai tra con du — xem DebtRepository.sumOutstandingBySupplierId. */
  private BigDecimal outstandingDebt;

  private BigDecimal totalPurchased;
  private long orderCount;
  private java.time.Instant lastPurchaseAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public BigDecimal getOutstandingDebt() {
    return outstandingDebt;
  }

  public void setOutstandingDebt(BigDecimal outstandingDebt) {
    this.outstandingDebt = outstandingDebt;
  }

  public BigDecimal getTotalPurchased() {
    return totalPurchased;
  }

  public void setTotalPurchased(BigDecimal totalPurchased) {
    this.totalPurchased = totalPurchased;
  }

  public long getOrderCount() {
    return orderCount;
  }

  public void setOrderCount(long orderCount) {
    this.orderCount = orderCount;
  }

  public java.time.Instant getLastPurchaseAt() {
    return lastPurchaseAt;
  }

  public void setLastPurchaseAt(java.time.Instant lastPurchaseAt) {
    this.lastPurchaseAt = lastPurchaseAt;
  }
}
