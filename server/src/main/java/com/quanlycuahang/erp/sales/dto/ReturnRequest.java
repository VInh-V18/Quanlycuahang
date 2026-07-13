package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public class ReturnRequest {

  @NotNull private Long orderId;

  @NotEmpty @Valid private java.util.List<ReturnItemRequest> items;

  /**
   * Rang buoc tap gia tri nhu OrderPaymentRequest.method - doi soat tien mat cuoi ca chi tru cac
   * phieu hoan co refund_method dung bang 'cash', gia tri la truoc day van duoc nhan va am tham lam
   * lech doi soat (phat hien khi rieng soat). Van cho phep null (tru vao cong no, khong hoan).
   */
  @Pattern(
      regexp = "cash|bank_transfer|card",
      message = "Phương thức hoàn tiền phải là cash, bank_transfer hoặc card")
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
