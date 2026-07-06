package com.quanlycuahang.erp.sales.dto;

import java.math.BigDecimal;
import java.util.List;

public class ReturnResponse {

  private Long id;
  private Long orderId;
  private BigDecimal totalRefund;
  private String refundMethod;
  private List<ReturnItemResponse> items;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
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

  public List<ReturnItemResponse> getItems() {
    return items;
  }

  public void setItems(List<ReturnItemResponse> items) {
    this.items = items;
  }
}
