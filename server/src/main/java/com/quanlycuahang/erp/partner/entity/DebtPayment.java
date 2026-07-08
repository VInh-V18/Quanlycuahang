package com.quanlycuahang.erp.partner.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "debt_payments")
@SQLDelete(sql = "UPDATE debt_payments SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class DebtPayment extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "debt_id", nullable = false)
  private Debt debt;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "method")
  private String method;

  @Column(name = "note")
  private String note;

  @Column(name = "paid_at", nullable = false)
  private OffsetDateTime paidAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  public Debt getDebt() {
    return debt;
  }

  public void setDebt(Debt debt) {
    this.debt = debt;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public OffsetDateTime getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(OffsetDateTime paidAt) {
    this.paidAt = paidAt;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }
}
