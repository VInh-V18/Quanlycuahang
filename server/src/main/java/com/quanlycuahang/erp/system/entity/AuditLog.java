package com.quanlycuahang.erp.system.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.BaseEntity;
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

/**
 * Nhat ky audit (D3): ghi qua AOP @Aspect quanh method co @Audited (Phase 6), khong goi tay tung
 * cho. before/after luu JSON tho (String) anh xa cot JSONB qua @JdbcTypeCode(SqlTypes.JSON) — tinh
 * nang JSON mapping cua Hibernate 6.3+ (can kiem chung lai neu ha cap Hibernate version trong tuong
 * lai).
 */
@Entity
@Table(name = "audit_logs")
@SQLDelete(sql = "UPDATE audit_logs SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class AuditLog extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "user_id")
  private User user;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id")
  private Branch branch;

  @Column(name = "action", nullable = false)
  private String action;

  @Column(name = "entity_name")
  private String entityName;

  @Column(name = "entity_id")
  private Long entityId;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "before", columnDefinition = "jsonb")
  private String before;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "after", columnDefinition = "jsonb")
  private String after;

  public User getUser() {
    return user;
  }

  public void setUser(User user) {
    this.user = user;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public String getAction() {
    return action;
  }

  public void setAction(String action) {
    this.action = action;
  }

  public String getEntityName() {
    return entityName;
  }

  public void setEntityName(String entityName) {
    this.entityName = entityName;
  }

  public Long getEntityId() {
    return entityId;
  }

  public void setEntityId(Long entityId) {
    this.entityId = entityId;
  }

  public String getBefore() {
    return before;
  }

  public void setBefore(String before) {
    this.before = before;
  }

  public String getAfter() {
    return after;
  }

  public void setAfter(String after) {
    this.after = after;
  }
}
