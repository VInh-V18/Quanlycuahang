package com.quanlycuahang.erp.sales.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderResponse {

  private Long id;
  private String orderNumber;
  private String status;
  private Long branchId;
  private Long customerId;
  private BigDecimal subtotalAmount;
  private BigDecimal discountAmount;
  private BigDecimal vatAmount;
  private BigDecimal roundingAdjustment;
  private BigDecimal totalAmount;
  private BigDecimal changeAmount;
  private BigDecimal shippingFee;
  private String note;
  private List<OrderItemResponse> items;
  private List<OrderPaymentResponse> payments;
  private Long invoiceId;
  private String invoiceNumber;
  private String qrPayload;
  private String lookupCode;
  private Instant createdAt;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public Long getCustomerId() {
    return customerId;
  }

  public void setCustomerId(Long customerId) {
    this.customerId = customerId;
  }

  public BigDecimal getSubtotalAmount() {
    return subtotalAmount;
  }

  public void setSubtotalAmount(BigDecimal subtotalAmount) {
    this.subtotalAmount = subtotalAmount;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(BigDecimal discountAmount) {
    this.discountAmount = discountAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public void setVatAmount(BigDecimal vatAmount) {
    this.vatAmount = vatAmount;
  }

  public BigDecimal getRoundingAdjustment() {
    return roundingAdjustment;
  }

  public void setRoundingAdjustment(BigDecimal roundingAdjustment) {
    this.roundingAdjustment = roundingAdjustment;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public BigDecimal getChangeAmount() {
    return changeAmount;
  }

  public void setChangeAmount(BigDecimal changeAmount) {
    this.changeAmount = changeAmount;
  }

  public BigDecimal getShippingFee() {
    return shippingFee;
  }

  public void setShippingFee(BigDecimal shippingFee) {
    this.shippingFee = shippingFee;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public List<OrderItemResponse> getItems() {
    return items;
  }

  public void setItems(List<OrderItemResponse> items) {
    this.items = items;
  }

  public List<OrderPaymentResponse> getPayments() {
    return payments;
  }

  public void setPayments(List<OrderPaymentResponse> payments) {
    this.payments = payments;
  }

  public Long getInvoiceId() {
    return invoiceId;
  }

  public void setInvoiceId(Long invoiceId) {
    this.invoiceId = invoiceId;
  }

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public void setInvoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
  }

  public String getQrPayload() {
    return qrPayload;
  }

  public void setQrPayload(String qrPayload) {
    this.qrPayload = qrPayload;
  }

  public String getLookupCode() {
    return lookupCode;
  }

  public void setLookupCode(String lookupCode) {
    this.lookupCode = lookupCode;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
