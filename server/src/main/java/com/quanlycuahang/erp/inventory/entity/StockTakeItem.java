package com.quanlycuahang.erp.inventory.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import com.quanlycuahang.erp.product.entity.Product;
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
@Table(name = "stock_take_items")
@SQLDelete(sql = "UPDATE stock_take_items SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class StockTakeItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "stock_take_id", nullable = false)
  private StockTake stockTake;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "expected_qty", nullable = false)
  private BigDecimal expectedQty;

  @Column(name = "actual_qty")
  private BigDecimal actualQty;

  @Column(name = "reason")
  private String reason;

  public StockTake getStockTake() {
    return stockTake;
  }

  public void setStockTake(StockTake stockTake) {
    this.stockTake = stockTake;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public BigDecimal getExpectedQty() {
    return expectedQty;
  }

  public void setExpectedQty(BigDecimal expectedQty) {
    this.expectedQty = expectedQty;
  }

  public BigDecimal getActualQty() {
    return actualQty;
  }

  public void setActualQty(BigDecimal actualQty) {
    this.actualQty = actualQty;
  }

  public String getReason() {
    return reason;
  }

  public void setReason(String reason) {
    this.reason = reason;
  }
}
