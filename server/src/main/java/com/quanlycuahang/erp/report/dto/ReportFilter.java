package com.quanlycuahang.erp.report.dto;

import java.time.LocalDate;

/** Bo loc dung chung cho moi bao cao (Phase 10) — khoang ngay + chi nhanh (null = tat ca). */
public class ReportFilter {

  private LocalDate from;
  private LocalDate to;
  private Long branchId;

  public LocalDate getFrom() {
    return from;
  }

  public void setFrom(LocalDate from) {
    this.from = from;
  }

  public LocalDate getTo() {
    return to;
  }

  public void setTo(LocalDate to) {
    this.to = to;
  }

  public Long getBranchId() {
    return branchId;
  }

  public void setBranchId(Long branchId) {
    this.branchId = branchId;
  }
}
