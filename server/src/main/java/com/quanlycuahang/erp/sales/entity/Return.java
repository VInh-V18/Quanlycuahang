package com.quanlycuahang.erp.sales.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/** Phieu tra hang theo hoa don goc (UC-12). */
@Entity
@Table(name = "returns")
@SQLDelete(sql = "UPDATE returns SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Return extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "created_by", nullable = false)
  private User createdBy;

  @Column(name = "total_refund", nullable = false)
  private BigDecimal totalRefund = BigDecimal.ZERO;

  @Column(name = "refund_method")
  private String refundMethod;

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }

  public BigDecimal getTotalRefund() {
    return totalRefund;
  }

  public void setTotalRefund(BigDecimal totalRefund) {
    this.totalRefund = totalRefund;
  }

  public String getRefundMethod() {
    return refundMethod;
  }

  public void setRefundMethod(String refundMethod) {
    this.refundMethod = refundMethod;
  }
}
