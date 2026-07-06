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
@Table(name = "purchase_order_items")
@SQLDelete(sql = "UPDATE purchase_order_items SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class PurchaseOrderItem extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "purchase_order_id", nullable = false)
  private PurchaseOrder purchaseOrder;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "unit_price", nullable = false)
  private BigDecimal unitPrice;

  public PurchaseOrder getPurchaseOrder() {
    return purchaseOrder;
  }

  public void setPurchaseOrder(PurchaseOrder purchaseOrder) {
    this.purchaseOrder = purchaseOrder;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitPrice() {
    return unitPrice;
  }

  public void setUnitPrice(BigDecimal unitPrice) {
    this.unitPrice = unitPrice;
  }
}
