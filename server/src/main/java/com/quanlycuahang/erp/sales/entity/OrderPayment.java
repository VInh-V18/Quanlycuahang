package com.quanlycuahang.erp.sales.entity;

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

/** Mot don co the co nhieu phuong thuc thanh toan (B4). method: cash, bank_transfer, card. */
@Entity
@Table(name = "order_payments")
@SQLDelete(sql = "UPDATE order_payments SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class OrderPayment extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "method", nullable = false)
  private String method;

  @Column(name = "amount", nullable = false)
  private BigDecimal amount;

  @Column(name = "qr_payload", columnDefinition = "text")
  private String qrPayload;

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public String getMethod() {
    return method;
  }

  public void setMethod(String method) {
    this.method = method;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }

  public String getQrPayload() {
    return qrPayload;
  }

  public void setQrPayload(String qrPayload) {
    this.qrPayload = qrPayload;
  }
}
