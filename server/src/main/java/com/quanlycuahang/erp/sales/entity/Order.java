package com.quanlycuahang.erp.sales.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.BaseEntity;
import com.quanlycuahang.erp.operation.entity.Shift;
import com.quanlycuahang.erp.partner.entity.Customer;
import com.quanlycuahang.erp.promotion.entity.Voucher;
import com.quanlycuahang.erp.system.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Don ban hang (UC-04). Trang thai theo state machine B4: draft -> completed -> partially_returned
 * -> fully_returned; draft -> cancelled; completed -> cancelled (validate tap trung o
 * OrderStatus.canTransition, khong rai if-else o Service — Phase 8).
 */
@Entity
@Table(name = "orders")
@SQLDelete(sql = "UPDATE orders SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Order extends BaseEntity {

  @Column(name = "order_number", nullable = false, unique = true)
  private String orderNumber;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "cashier_id", nullable = false)
  private User cashier;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "shift_id")
  private Shift shift;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "voucher_id")
  private Voucher voucher;

  @Column(name = "status", nullable = false)
  private String status = "draft";

  @Column(name = "subtotal_amount", nullable = false)
  private BigDecimal subtotalAmount = BigDecimal.ZERO;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "vat_amount", nullable = false)
  private BigDecimal vatAmount = BigDecimal.ZERO;

  @Column(name = "rounding_adjustment", nullable = false)
  private BigDecimal roundingAdjustment = BigDecimal.ZERO;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount = BigDecimal.ZERO;

  public String getOrderNumber() {
    return orderNumber;
  }

  public void setOrderNumber(String orderNumber) {
    this.orderNumber = orderNumber;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public User getCashier() {
    return cashier;
  }

  public void setCashier(User cashier) {
    this.cashier = cashier;
  }

  public Shift getShift() {
    return shift;
  }

  public void setShift(Shift shift) {
    this.shift = shift;
  }

  public Voucher getVoucher() {
    return voucher;
  }

  public void setVoucher(Voucher voucher) {
    this.voucher = voucher;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
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
}
