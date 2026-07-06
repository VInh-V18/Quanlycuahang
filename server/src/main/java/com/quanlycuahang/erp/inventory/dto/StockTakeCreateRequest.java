package com.quanlycuahang.erp.inventory.dto;

import jakarta.validation.constraints.NotNull;

public class StockTakeCreateRequest {

  @NotNull private Long branchId;

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }
}
