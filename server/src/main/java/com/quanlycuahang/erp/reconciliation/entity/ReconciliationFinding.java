package com.quanlycuahang.erp.reconciliation.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

/** 1 chỗ lệch cụ thể phát hiện được trong 1 {@link ReconciliationRun} (Prompt #6). */
@Entity
@Table(name = "reconciliation_findings")
@SQLDelete(sql = "UPDATE reconciliation_findings SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ReconciliationFinding extends TenantScopedEntity {

  public static final String STATUS_OPEN = "open";
  public static final String STATUS_ACKNOWLEDGED = "acknowledged";
  public static final String STATUS_RESOLVED = "resolved";

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "run_id", nullable = false)
  private ReconciliationRun run;

  @Column(name = "check_type", nullable = false)
  private String checkType;

  @Column(name = "severity", nullable = false)
  private String severity;

  @Column(name = "entity_type")
  private String entityType;

  @Column(name = "entity_id")
  private Long entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "details", columnDefinition = "jsonb")
  private String details;

  @Column(name = "status", nullable = false)
  private String status = STATUS_OPEN;

  public ReconciliationRun getRun() {
    return run;
  }

  public void setRun(ReconciliationRun run) {
    this.run = run;
  }

  public String getCheckType() {
    return checkType;
  }

  public void setCheckType(String checkType) {
    this.checkType = checkType;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public String getEntityType() {
    return entityType;
  }

  public void setEntityType(String entityType) {
    this.entityType = entityType;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getDetails() {
    return details;
  }

  public void setDetails(String details) {
    this.details = details;
  }

  public String getStatus() {
    return status;
  }

  public void setStatus(String status) {
    this.status = status;
  }
}
