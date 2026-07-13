package com.quanlycuahang.erp.operation.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import com.quanlycuahang.erp.system.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "shifts")
@SQLDelete(sql = "UPDATE shifts SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Shift extends TenantScopedEntity {

  // Chong dong ca 2 lan dong thoi (khong co truoc day - 2 request cung dong 1 ca co the cung doc
  // status="open", request thu 2 am tham ghi de actualCash/discrepancy/note thay vi bao loi ro
  // rang - phat hien khi rieng soat, giong pattern @Version da co san tren Inventory/Voucher).
  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "opened_by", nullable = false)
  private User openedBy;

  @Column(name = "opening_cash", nullable = false)
  private BigDecimal openingCash;

  @Column(name = "actual_cash")
  private BigDecimal actualCash;

  @Column(name = "discrepancy")
  private BigDecimal discrepancy;

  @Column(name = "note")
  private String note;

  @Column(name = "status", nullable = false)
  private String status = "open";

  @Column(name = "opened_at", nullable = false)
  private OffsetDateTime openedAt;

  @Column(name = "closed_at")
  private OffsetDateTime closedAt;

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public User getOpenedBy() {
    return openedBy;
  }

  public void setOpenedBy(User openedBy) {
    this.openedBy = openedBy;
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

  public OffsetDateTime getOpenedAt() {
    return openedAt;
  }

  public void setOpenedAt(OffsetDateTime openedAt) {
    this.openedAt = openedAt;
  }

  public OffsetDateTime getClosedAt() {
    return closedAt;
  }

  public void setClosedAt(OffsetDateTime closedAt) {
    this.closedAt = closedAt;
  }
}
