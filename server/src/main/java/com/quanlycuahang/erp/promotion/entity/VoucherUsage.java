package com.quanlycuahang.erp.promotion.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import com.quanlycuahang.erp.sales.entity.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "voucher_usages")
@SQLDelete(sql = "UPDATE voucher_usages SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class VoucherUsage extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "voucher_id", nullable = false)
  private Voucher voucher;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @Column(name = "used_at", nullable = false)
  private OffsetDateTime usedAt;

  public Voucher getVoucher() {
    return voucher;
  }

  public void setVoucher(Voucher voucher) {
    this.voucher = voucher;
  }

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public OffsetDateTime getUsedAt() {
    return usedAt;
  }

  public void setUsedAt(OffsetDateTime usedAt) {
    this.usedAt = usedAt;
  }
}
