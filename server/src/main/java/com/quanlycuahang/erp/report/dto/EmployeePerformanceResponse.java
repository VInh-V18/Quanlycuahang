package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

public class EmployeePerformanceResponse {

  private Long cashierId;
  private String cashierName;
  private long orderCount;
  private BigDecimal revenue;
  private BigDecimal averageOrderValue;

  public EmployeePerformanceResponse() {}

  public EmployeePerformanceResponse(
      Long cashierId, String cashierName, long orderCount, BigDecimal revenue) {
    this.cashierId = cashierId;
    this.cashierName = cashierName;
    this.orderCount = orderCount;
    this.revenue = revenue;
    this.averageOrderValue =
        orderCount == 0
            ? BigDecimal.ZERO
            : revenue.divide(BigDecimal.valueOf(orderCount), 0, java.math.RoundingMode.HALF_UP);
  }

  public Long getCashierId() {
    return cashierId;
  }

  public void setCashierId(Long cashierId) {
    this.cashierId = cashierId;
  }

  public String getCashierName() {
    return cashierName;
  }

  public void setCashierName(String cashierName) {
    this.cashierName = cashierName;
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

  public BigDecimal getAverageOrderValue() {
    return averageOrderValue;
  }

  public void setAverageOrderValue(BigDecimal averageOrderValue) {
    this.averageOrderValue = averageOrderValue;
  }
}
