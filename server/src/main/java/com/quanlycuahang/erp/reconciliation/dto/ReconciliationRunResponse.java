package com.quanlycuahang.erp.reconciliation.dto;

import java.time.OffsetDateTime;
import java.util.List;

public class ReconciliationRunResponse {

  private Long id;
  private String triggerType;
  private String status;
  private int findingsCount;
  private Long durationMs;
  private OffsetDateTime startedAt;
  private OffsetDateTime finishedAt;
  private List<ReconciliationFindingResponse> findings;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getTriggerType() {
    return triggerType;
  }

  public void setTriggerType(String triggerType) {
    this.triggerType = triggerType;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }

  public int getFindingsCount() {
    return findingsCount;
  }

  public void setFindingsCount(int findingsCount) {
    this.findingsCount = findingsCount;
  }

  public Long getDurationMs() {
    return durationMs;
  }

  public void setDurationMs(Long durationMs) {
    this.durationMs = durationMs;
  }

  public OffsetDateTime getStartedAt() {
    return startedAt;
  }

  public void setStartedAt(OffsetDateTime startedAt) {
    this.startedAt = startedAt;
  }

  public OffsetDateTime getFinishedAt() {
    return finishedAt;
  }

  public void setFinishedAt(OffsetDateTime finishedAt) {
    this.finishedAt = finishedAt;
  }

  public List<ReconciliationFindingResponse> getFindings() {
    return findings;
  }

  public void setFindings(List<ReconciliationFindingResponse> findings) {
    this.findings = findings;
  }
}
