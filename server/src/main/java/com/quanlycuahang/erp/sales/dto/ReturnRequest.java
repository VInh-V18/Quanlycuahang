package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

public class ReturnRequest {

  @NotNull private Long orderId;

  @NotEmpty @Valid private java.util.List<ReturnItemRequest> items;

  private String refundMethod;

  public Long getOrderId() {
    return orderId;
  }

  public void setOrderId(Long orderId) {
    this.orderId = orderId;
  }

  public java.util.List<ReturnItemRequest> getItems() {
    return items;
  }

  public void setItems(java.util.List<ReturnItemRequest> items) {
    this.items = items;
  }

  public String getRefundMethod() {
    return refundMethod;
  }

  public void setRefundMethod(String refundMethod) {
    this.refundMethod = refundMethod;
  }
}
