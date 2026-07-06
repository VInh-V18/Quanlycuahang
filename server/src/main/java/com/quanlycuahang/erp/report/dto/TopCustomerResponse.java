package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

public class TopCustomerResponse {

  private Long customerId;
  private String customerName;
  private long orderCount;
  private BigDecimal revenue;

  public TopCustomerResponse() {}

  public TopCustomerResponse(
      Long customerId, String customerName, long orderCount, BigDecimal revenue) {
    this.customerId = customerId;
    this.customerName = customerName;
    this.orderCount = orderCount;
    this.revenue = revenue;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public String getCustomerName() {
    return customerName;
  }

  public void setCustomerName(String customerName) {
    this.customerName = customerName;
  }

  public long getOrderCount() {
    return orderCount;
  }

  public void setOrderCount(long orderCount) {
    this.orderCount = orderCount;
  }

  public BigDecimal getRevenue() {
    return revenue;
  }

  public void setRevenue(BigDecimal revenue) {
    this.revenue = revenue;
  }
}
