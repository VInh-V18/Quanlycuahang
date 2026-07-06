package com.quanlycuahang.erp.operation.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class ShiftSummaryResponse {

  private Long id;
  private String branchName;
  private String cashierName;
  private BigDecimal openingCash;
  private BigDecimal actualCash;
  private BigDecimal discrepancy;
  private String note;
  private String status;
  private Instant openedAt;
  private Instant closedAt;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public String getCashierName() {
    return cashierName;
  }

  public void setCashierName(String cashierName) {
    this.cashierName = cashierName;
  }

  public BigDecimal getOpeningCash() {
    return openingCash;
  }

  public void setOpeningCash(BigDecimal openingCash) {
    this.openingCash = openingCash;
  }

  public BigDecimal getActualCash() {
    return actualCash;
  }

  public void setActualCash(BigDecimal actualCash) {
    this.actualCash = actualCash;
  }

  public BigDecimal getDiscrepancy() {
    return discrepancy;
  }

  public void setDiscrepancy(BigDecimal discrepancy) {
    this.discrepancy = discrepancy;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public Instant getOpenedAt() {
    return openedAt;
  }

  public void setOpenedAt(Instant openedAt) {
    this.openedAt = openedAt;
  }

  public Instant getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(Instant closedAt) {
    this.closedAt = closedAt;
  }
}
