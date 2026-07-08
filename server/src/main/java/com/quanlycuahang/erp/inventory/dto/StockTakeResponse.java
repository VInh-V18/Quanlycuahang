package com.quanlycuahang.erp.inventory.dto;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;

public class StockTakeResponse {

  private Long id;
  private Long branchId;
  private String status;
  private Instant createdAt;
  private OffsetDateTime approvedAt;
  private List<StockTakeItemResponse> items;

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

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Instant createdAt) {
    this.createdAt = createdAt;
  }

  public OffsetDateTime getApprovedAt() {
    return approvedAt;
  }

  public void setApprovedAt(OffsetDateTime approvedAt) {
    this.approvedAt = approvedAt;
  }

  public List<StockTakeItemResponse> getItems() {
    return items;
  }

  public void setItems(List<StockTakeItemResponse> items) {
    this.items = items;
  }
}
