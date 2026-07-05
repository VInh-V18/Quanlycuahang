package com.quanlycuahang.erp.inventory.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class PurchaseOrderResponse {

  private Long id;
  private Long supplierId;
  private String supplierName;
  private Long branchId;
  private String status;
  private BigDecimal totalAmount;
  private List<PurchaseOrderItemResponse> items;
  private Instant createdAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getSupplierId() {
    return supplierId;
  }

  public void setSupplierId(Long supplierId) {
    this.supplierId = supplierId;
  }

  public String getSupplierName() {
    return supplierName;
  }

  public void setSupplierName(String supplierName) {
    this.supplierName = supplierName;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
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

  public List<PurchaseOrderItemResponse> getItems() {
    return items;
  }

  public void setItems(List<PurchaseOrderItemResponse> items) {
    this.items = items;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }
}
