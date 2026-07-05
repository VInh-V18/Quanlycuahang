package com.quanlycuahang.erp.partner.entity;

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

/**
 * Cong no 2 chieu: receivable (khach hang no) hoac payable (phai tra NCC). Dung dung 1 trong 2 quan
 * he customer/supplier tuy direction (D5, UC-14/15).
 */
@Entity
@Table(name = "debts")
@SQLDelete(sql = "UPDATE debts SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Debt extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_id")
  private Customer customer;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "supplier_id")
  private Supplier supplier;

  @Column(name = "direction", nullable = false)
  private String direction;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "reference_type")
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "note")
  private String note;

  public Customer getCustomer() {
    return customer;
  }

  public void setCustomer(Customer customer) {
    this.customer = customer;
  }

  public Supplier getSupplier() {
    return supplier;
  }

  public void setSupplier(Supplier supplier) {
    this.supplier = supplier;
  }

  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(String referenceType) {
    this.referenceType = referenceType;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(Long referenceId) {
    this.referenceId = referenceId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
