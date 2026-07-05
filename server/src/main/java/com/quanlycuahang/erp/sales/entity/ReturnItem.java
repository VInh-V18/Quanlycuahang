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

@Entity
@Table(name = "return_items")
@SQLDelete(sql = "UPDATE return_items SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class ReturnItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "return_id", nullable = false)
  private Return returnEntity;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_item_id", nullable = false)
  private OrderItem orderItem;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "refund_amount", nullable = false)
  private BigDecimal refundAmount;

  public Return getReturnEntity() {
    return returnEntity;
  }

  public void setReturnEntity(Return returnEntity) {
    this.returnEntity = returnEntity;
  }

  public OrderItem getOrderItem() {
    return orderItem;
  }

  public void setOrderItem(OrderItem orderItem) {
    this.orderItem = orderItem;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getRefundAmount() {
    return refundAmount;
  }

  public void setRefundAmount(BigDecimal refundAmount) {
    this.refundAmount = refundAmount;
  }
}
