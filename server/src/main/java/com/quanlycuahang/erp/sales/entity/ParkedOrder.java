package com.quanlycuahang.erp.sales.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import com.quanlycuahang.erp.system.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.hibernate.type.SqlTypes;

/** Don treo POS (UC-18) — chua tru kho, chua phai Order chinh thuc. */
@Entity
@Table(name = "parked_orders")
@SQLDelete(sql = "UPDATE parked_orders SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ParkedOrder extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "cart_snapshot", nullable = false, columnDefinition = "jsonb")
  private String cartSnapshot;

  @Column(name = "note")
  private String note;

  @Column(name = "parked_at", nullable = false)
  private OffsetDateTime parkedAt;

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
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
