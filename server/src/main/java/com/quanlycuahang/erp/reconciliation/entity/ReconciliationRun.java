package com.quanlycuahang.erp.reconciliation.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 1 lan chay doi soat toan ven du lieu (Prompt #6) - thu cong (POST /admin/reconciliation/run) hoac
 * job dem (ReconciliationScheduledJob), luon gioi han 1 tenant duy nhat moi lan chay.
 */
@Entity
@Table(name = "reconciliation_runs")
@SQLDelete(sql = "UPDATE reconciliation_runs SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ReconciliationRun extends TenantScopedEntity {

  public static final String TRIGGER_MANUAL = "manual";
  public static final String TRIGGER_SCHEDULED = "scheduled";

  public static final String STATUS_RUNNING = "running";
  public static final String STATUS_COMPLETED = "completed";
  public static final String STATUS_FAILED = "failed";

  @Column(name = "trigger_type", nullable = false)
  private String triggerType;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "triggered_by")
  private User triggeredBy;

  @Column(name = "started_at", nullable = false)
  private OffsetDateTime startedAt;

  @Column(name = "finished_at")
  private OffsetDateTime finishedAt;

  @Column(name = "status", nullable = false)
  private String status;

  @Column(name = "findings_count", nullable = false)
  private int findingsCount;

  @Column(name = "duration_ms")
  private Long durationMs;

  public String getTriggerType() {
    return triggerType;
  }

  public void setTriggerType(String triggerType) {
    this.triggerType = triggerType;
  }

  public User getTriggeredBy() {
    return triggeredBy;
  }

  public void setTriggeredBy(User triggeredBy) {
    this.triggeredBy = triggeredBy;
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
}
