package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class ParkedOrderRequest {

  @NotNull private Long branchId;

  /** Giu nguyen dang JSON string (gio hang FE tu quyet dinh cau truc) — Backend khong dien giai. */
  @NotBlank private String cartSnapshot;

  private String note;

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
}
