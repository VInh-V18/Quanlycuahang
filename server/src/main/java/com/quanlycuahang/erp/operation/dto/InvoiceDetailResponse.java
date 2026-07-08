package com.quanlycuahang.erp.operation.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

/**
 * Du lieu hoa don day du dang JSON (Phase 9) — Backend CHI tra JSON, khong render HTML/PDF (B3).
 * Frontend tu ve template in K80/A4 tu du lieu nay.
 */
public class InvoiceDetailResponse {

  private String invoiceNumber;
  private OffsetDateTime issuedAt;
  private String lookupCode;
  private String qrPayload;

  private String orderNumber;
  private String orderStatus;

  private String storeName;
  private String storeTaxCode;
  private String storePhone;
  private String bankAccountName;
  private String bankAccountNumber;
  private String bankName;
  private String bankQrImageUrl;
  private String branchName;
  private String branchAddress;
  private String branchPhone;

  private String cashierName;
  private String customerName;
  private String customerPhone;
  private String customerEmail;
  private String customerAddress;

  private List<InvoiceLineResponse> lines;
  private List<VatBreakdownResponse> vatBreakdown;
  private List<InvoicePaymentResponse> payments;

  private BigDecimal subtotalAmount;
  private BigDecimal discountAmount;
  private BigDecimal vatAmount;
  private BigDecimal roundingAdjustment;
  private BigDecimal shippingFee;
  private BigDecimal totalAmount;
  private BigDecimal totalQuantity;
  private BigDecimal cashReceived;
  private BigDecimal changeAmount;
  private String note;

  public String getInvoiceNumber() {
    return invoiceNumber;
  }

  public void setInvoiceNumber(String invoiceNumber) {
    this.invoiceNumber = invoiceNumber;
  }

  public OffsetDateTime getIssuedAt() {
    return issuedAt;
  }

  public void setIssuedAt(OffsetDateTime issuedAt) {
    this.issuedAt = issuedAt;
  }

  public String getLookupCode() {
    return lookupCode;
  }

  public void setLookupCode(String lookupCode) {
    this.lookupCode = lookupCode;
  }

  public String getQrPayload() {
    return qrPayload;
  }

  public void setQrPayload(String qrPayload) {
    this.qrPayload = qrPayload;
  }

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public String getOrderStatus() {
    return orderStatus;
  }

  public void setOrderStatus(String orderStatus) {
    this.orderStatus = orderStatus;
  }

  public String getStoreName() {
    return storeName;
  }

  public void setStoreName(String storeName) {
    this.storeName = storeName;
  }

  public String getStoreTaxCode() {
    return storeTaxCode;
  }

  public void setStoreTaxCode(String storeTaxCode) {
    this.storeTaxCode = storeTaxCode;
  }

  public String getStorePhone() {
    return storePhone;
  }

  public void setStorePhone(String storePhone) {
    this.storePhone = storePhone;
  }

  public String getBankAccountName() {
    return bankAccountName;
  }

  public void setBankAccountName(String bankAccountName) {
    this.bankAccountName = bankAccountName;
  }

  public String getBankAccountNumber() {
    return bankAccountNumber;
  }

  public void setBankAccountNumber(String bankAccountNumber) {
    this.bankAccountNumber = bankAccountNumber;
  }

  public String getBankName() {
    return bankName;
  }

  public void setBankName(String bankName) {
    this.bankName = bankName;
  }

  public String getBankQrImageUrl() {
    return bankQrImageUrl;
  }

  public void setBankQrImageUrl(String bankQrImageUrl) {
    this.bankQrImageUrl = bankQrImageUrl;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public String getBranchAddress() {
    return branchAddress;
  }

  public void setBranchAddress(String branchAddress) {
    this.branchAddress = branchAddress;
  }

  public String getBranchPhone() {
    return branchPhone;
  }

  public void setBranchPhone(String branchPhone) {
    this.branchPhone = branchPhone;
  }

  public String getCashierName() {
    return cashierName;
  }

  public void setCashierName(String cashierName) {
    this.cashierName = cashierName;
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

  public String getCustomerEmail() {
    return customerEmail;
  }

  public void setCustomerEmail(String customerEmail) {
    this.customerEmail = customerEmail;
  }

  public String getCustomerAddress() {
    return customerAddress;
  }

  public void setCustomerAddress(String customerAddress) {
    this.customerAddress = customerAddress;
  }

  public List<InvoiceLineResponse> getLines() {
    return lines;
  }

  public void setLines(List<InvoiceLineResponse> lines) {
    this.lines = lines;
  }

  public List<VatBreakdownResponse> getVatBreakdown() {
    return vatBreakdown;
  }

  public void setVatBreakdown(List<VatBreakdownResponse> vatBreakdown) {
    this.vatBreakdown = vatBreakdown;
  }

  public List<InvoicePaymentResponse> getPayments() {
    return payments;
  }

  public void setPayments(List<InvoicePaymentResponse> payments) {
    this.payments = payments;
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

  public BigDecimal getShippingFee() {
    return shippingFee;
  }

  public void setShippingFee(BigDecimal shippingFee) {
    this.shippingFee = shippingFee;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public BigDecimal getTotalQuantity() {
    return totalQuantity;
  }

  public void setTotalQuantity(BigDecimal totalQuantity) {
    this.totalQuantity = totalQuantity;
  }

  public BigDecimal getCashReceived() {
    return cashReceived;
  }

  public void setCashReceived(BigDecimal cashReceived) {
    this.cashReceived = cashReceived;
  }

  public BigDecimal getChangeAmount() {
    return changeAmount;
  }

  public void setChangeAmount(BigDecimal changeAmount) {
    this.changeAmount = changeAmount;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
