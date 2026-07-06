package com.quanlycuahang.erp.dashboard.dto;

import java.math.BigDecimal;

public class RecentOrderResponse {

  private String orderNumber;
  private String customerName;
  private BigDecimal totalAmount;
  private String status;

  public RecentOrderResponse() {}

  public RecentOrderResponse(
      String orderNumber, String customerName, BigDecimal totalAmount, String status) {
    this.orderNumber = orderNumber;
    this.customerName = customerName;
    this.totalAmount = totalAmount;
    this.status = status;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
