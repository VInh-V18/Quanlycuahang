package com.quanlycuahang.erp.product.entity;

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

/** Lich su thay doi gia ban — ghi qua @EntityListeners khi Product.sellPrice doi (Phase 7.1). */
@Entity
@Table(name = "price_history")
@SQLDelete(sql = "UPDATE price_history SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class PriceHistory extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "old_price")
  private BigDecimal oldPrice;

  @Column(name = "new_price", nullable = false)
  private BigDecimal newPrice;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "changed_by")
  private User changedBy;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public BigDecimal getOldPrice() {
    return oldPrice;
  }

  public void setOldPrice(BigDecimal oldPrice) {
    this.oldPrice = oldPrice;
  }

  public BigDecimal getNewPrice() {
    return newPrice;
  }

  public void setNewPrice(BigDecimal newPrice) {
    this.newPrice = newPrice;
  }

  public User getChangedBy() {
    return changedBy;
  }

  public void setChangedBy(User changedBy) {
    this.changedBy = changedBy;
  }
}
