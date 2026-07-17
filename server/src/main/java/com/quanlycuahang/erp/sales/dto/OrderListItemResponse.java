package com.quanlycuahang.erp.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderListItemResponse {

  private Long id;
  private String orderNumber;
  private Instant createdAt;
  private String status;
  private BigDecimal totalAmount;
  private String customerName;
  private String customerPhone;
  private String cashierName;
  private boolean hasDebt;
  private List<String> paymentMethods;

  /**
   * Nhan tieng Viet cua status (vd "completed" -> "Hoàn thành") — tinh 1 lan o
   * OrderService.toListItem(), dung chung cho man hinh Don hang (StatusBadge tu dich rieng qua
   * client/src/lib/orderStatus.ts, khong doc field nay) lan file Excel xuat ra (truoc day
   * OrderController tu dich lai rieng, phat hien khi rieng soat co the lech nhau).
   */
  private String statusLabel;

  /**
   * "Ghi nợ" hoac cac phuong thuc thanh toan noi boi " + " — tinh 1 lan o
   * OrderService.toListItem(), dung cho file Excel xuat ra (man hinh dung rieng StatusBadge/badge
   * mau cho cung du lieu hasDebt/paymentMethods, khong doc field nay).
   */
  private String paymentLabel;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
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

  public String getCashierName() {
    return cashierName;
  }

  public void setCashierName(String cashierName) {
    this.cashierName = cashierName;
  }

  public boolean isHasDebt() {
    return hasDebt;
  }

  public void setHasDebt(boolean hasDebt) {
    this.hasDebt = hasDebt;
  }

  public List<String> getPaymentMethods() {
    return paymentMethods;
  }

  public void setPaymentMethods(List<String> paymentMethods) {
    this.paymentMethods = paymentMethods;
  }

  public String getStatusLabel() {
    return statusLabel;
  }

  public void setStatusLabel(String statusLabel) {
    this.statusLabel = statusLabel;
  }

  public String getPaymentLabel() {
    return paymentLabel;
  }

  public void setPaymentLabel(String paymentLabel) {
    this.paymentLabel = paymentLabel;
  }
}
