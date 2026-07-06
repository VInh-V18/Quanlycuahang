package com.quanlycuahang.erp.sales.dto;

import java.time.OffsetDateTime;

public class ParkedOrderResponse {

  private Long id;
  private Long branchId;
  private String cartSnapshot;
  private String note;
  private OffsetDateTime parkedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }

  public String getCartSnapshot() {
    return cartSnapshot;
  }

  public void setCartSnapshot(String cartSnapshot) {
    this.cartSnapshot = cartSnapshot;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public OffsetDateTime getParkedAt() {
    return parkedAt;
  }

  public void setParkedAt(OffsetDateTime parkedAt) {
    this.parkedAt = parkedAt;
  }
}
