package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * expectedTotalAmount: tong tien FE da tinh (theo cung cong thuc B4) — Backend tinh lai doc lap
 * bang OrderPricingService, lech thi reject ORDER_PRICE_MISMATCH (UC-04 buoc 7).
 */
public class OrderCreateRequest {

  @NotNull private Long branchId;

  private Long customerId;

  private Long shiftId;

  private String voucherCode;

  @NotNull @PositiveOrZero private BigDecimal orderDiscountAmount = BigDecimal.ZERO;

  private BigDecimal cashReceived;

  @NotNull private BigDecimal expectedTotalAmount;

  @NotNull @PositiveOrZero private BigDecimal shippingFee = BigDecimal.ZERO;

  @Size(max = 500)
  private String note;

  @NotEmpty @Valid private List<OrderLineRequest> lines;

  /** Duoc phep rong (khong bat @NotEmpty) — rong nghia la chua thu tien gi, toan bo tien hang se
   * thanh cong no phai thu cua khach (bat buoc phai co customerId — xem OrderService, dong kiem tra
   * unpaid > 0 && customer == null). */
  @Valid private List<OrderPaymentRequest> payments = new ArrayList<>();

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

  public Long getShiftId() {
    return shiftId;
  }

  public void setShiftId(Long shiftId) {
    this.shiftId = shiftId;
  }

  public String getVoucherCode() {
    return voucherCode;
  }

  public void setVoucherCode(String voucherCode) {
    this.voucherCode = voucherCode;
  }

  public BigDecimal getOrderDiscountAmount() {
    return orderDiscountAmount;
  }

  public void setOrderDiscountAmount(BigDecimal orderDiscountAmount) {
    this.orderDiscountAmount = orderDiscountAmount;
  }

  public BigDecimal getCashReceived() {
    return cashReceived;
  }

  public void setCashReceived(BigDecimal cashReceived) {
    this.cashReceived = cashReceived;
  }

  public BigDecimal getExpectedTotalAmount() {
    return expectedTotalAmount;
  }

  public void setExpectedTotalAmount(BigDecimal expectedTotalAmount) {
    this.expectedTotalAmount = expectedTotalAmount;
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

  public List<OrderLineRequest> getLines() {
    return lines;
  }

  public void setLines(List<OrderLineRequest> lines) {
    this.lines = lines;
  }

  public List<OrderPaymentRequest> getPayments() {
    return payments;
  }

  public void setPayments(List<OrderPaymentRequest> payments) {
    this.payments = payments == null ? new ArrayList<>() : payments;
  }
}
