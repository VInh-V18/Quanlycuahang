package com.quanlycuahang.erp.operation.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/** Thu/chi tien mat ngoai don hang, gan voi 1 ca (UC-19). type: cash_in, cash_out. */
@Entity
@Table(name = "cash_transactions")
@SQLDelete(sql = "UPDATE cash_transactions SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class CashTransaction extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "shift_id", nullable = false)
  private Shift shift;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "note")
  private String note;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

  public Shift getShift() {
    return shift;
  }

  public void setShift(Shift shift) {
    this.shift = shift;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }
}
