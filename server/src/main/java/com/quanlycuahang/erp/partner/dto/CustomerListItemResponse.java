package com.quanlycuahang.erp.partner.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class CustomerListItemResponse {

  private Long id;
  private String name;
  private String phone;
  private String address;
  private Long customerGroupId;
  private String groupName;
  private BigDecimal debtLimit;
  private BigDecimal totalPurchased;
  private long orderCount;
  private Instant lastPurchaseAt;
  private BigDecimal currentDebt;

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

  public Long getCustomerGroupId() {
    return customerGroupId;
  }

  public void setCustomerGroupId(Long customerGroupId) {
    this.customerGroupId = customerGroupId;
  }

  public String getGroupName() {
    return groupName;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public BigDecimal getDebtLimit() {
    return debtLimit;
  }

  public void setDebtLimit(BigDecimal debtLimit) {
    this.debtLimit = debtLimit;
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

  public Instant getLastPurchaseAt() {
    return lastPurchaseAt;
  }

  public void setLastPurchaseAt(Instant lastPurchaseAt) {
    this.lastPurchaseAt = lastPurchaseAt;
  }

  public BigDecimal getCurrentDebt() {
    return currentDebt;
  }

  public void setCurrentDebt(BigDecimal currentDebt) {
    this.currentDebt = currentDebt;
  }
}
