package com.quanlycuahang.erp.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;
import java.util.List;

public class PurchaseOrderRequest {

  @NotNull private Long supplierId;

  @NotNull private Long branchId;

  @NotEmpty @Valid private List<PurchaseOrderItemRequest> items;

  /** So tien da tra ngay (co the < tong tien -> phan con lai ghi cong no phai tra NCC, B4). */
  @NotNull @PositiveOrZero private BigDecimal paidAmount = BigDecimal.ZERO;

  /** Chiet khau NCC tren tong tien hang (FH-6) — tru truoc khi tinh so con phai tra. */
  @PositiveOrZero private BigDecimal discountAmount = BigDecimal.ZERO;

  public Long getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(Long supplierId) {
    this.supplierId = supplierId;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public List<PurchaseOrderItemRequest> getItems() {
    return items;
  }

  public void setItems(List<PurchaseOrderItemRequest> items) {
    this.items = items;
  }

  public BigDecimal getPaidAmount() {
    return paidAmount;
  }

  public void setPaidAmount(BigDecimal paidAmount) {
    this.paidAmount = paidAmount;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(BigDecimal discountAmount) {
    this.discountAmount = discountAmount;
  }
}
