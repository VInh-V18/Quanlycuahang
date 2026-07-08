package com.quanlycuahang.erp.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class ReturnListItemResponse {

  private Long id;
  private Instant createdAt;
  private String orderNumber;
  private String customerName;
  private String customerPhone;
  private BigDecimal totalRefund;
  private String refundMethod;
  private Long itemCount;
  private String createdByName;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
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

  public String getCustomerPhone() {
    return customerPhone;
  }

  public void setCustomerPhone(String customerPhone) {
    this.customerPhone = customerPhone;
  }

  public BigDecimal getTotalRefund() {
    return totalRefund;
  }

  public void setTotalRefund(BigDecimal totalRefund) {
    this.totalRefund = totalRefund;
  }

  public String getRefundMethod() {
    return refundMethod;
  }

  public void setRefundMethod(String refundMethod) {
    this.refundMethod = refundMethod;
  }

  public Long getItemCount() {
    return itemCount;
  }

  public void setItemCount(Long itemCount) {
    this.itemCount = itemCount;
  }

  public String getCreatedByName() {
    return createdByName;
  }

  public void setCreatedByName(String createdByName) {
    this.createdByName = createdByName;
  }
}
