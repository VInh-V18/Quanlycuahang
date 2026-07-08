package com.quanlycuahang.erp.inventory.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.system.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Ton kho hien tai theo tung san pham + chi nhanh. @Version cho optimistic locking (B4 chong
 * oversell lop 1) — Hibernate tu them WHERE version = ? vao UPDATE, va cham nem
 * OptimisticLockException.
 */
@Entity
@Table(name = "inventory")
@SQLDelete(sql = "UPDATE inventory SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Inventory extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @Column(name = "stock", nullable = false)
  private BigDecimal stock;

  @Column(name = "cost_price", nullable = false)
  private BigDecimal costPrice;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public BigDecimal getStock() {
    return stock;
  }

  public void setStock(BigDecimal stock) {
    this.stock = stock;
  }

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
  }

  public Long getVersion() {
    return version;
  }

  public void setVersion(Long version) {
    this.version = version;
  }
}
