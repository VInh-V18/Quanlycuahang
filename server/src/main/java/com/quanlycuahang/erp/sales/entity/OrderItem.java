package com.quanlycuahang.erp.sales.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
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

/**
 * Dong don hang. Snapshot bat buoc productNameSnapshot/unitPriceSnapshot/
 * costPriceSnapshot/vatRateSnapshot (D5) — sua Product sau khi ban KHONG lam sai lich su hoa don cu
 * (khong join lai Product khi hien thi hoa don).
 */
@Entity
@Table(name = "order_items")
@SQLDelete(sql = "UPDATE order_items SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class OrderItem extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id", nullable = false)
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @Column(name = "product_name_snapshot", nullable = false)
  private String productNameSnapshot;

  @Column(name = "unit_price_snapshot", nullable = false)
  private BigDecimal unitPriceSnapshot;

  @Column(name = "cost_price_snapshot", nullable = false)
  private BigDecimal costPriceSnapshot;

  @Column(name = "vat_rate_snapshot", nullable = false)
  private BigDecimal vatRateSnapshot;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "discount_amount", nullable = false)
  private BigDecimal discountAmount = BigDecimal.ZERO;

  @Column(name = "vat_amount", nullable = false)
  private BigDecimal vatAmount = BigDecimal.ZERO;

  @Column(name = "line_total", nullable = false)
  private BigDecimal lineTotal;

  @Column(name = "returned_quantity", nullable = false)
  private BigDecimal returnedQuantity = BigDecimal.ZERO;

  public Order getOrder() {
    return order;
  }

  public void setOrder(Order order) {
    this.order = order;
  }

  public Product getProduct() {
    return product;
  }

  public void setProduct(Product product) {
    this.product = product;
  }

  public String getProductNameSnapshot() {
    return productNameSnapshot;
  }

  public void setProductNameSnapshot(String productNameSnapshot) {
    this.productNameSnapshot = productNameSnapshot;
  }

  public BigDecimal getUnitPriceSnapshot() {
    return unitPriceSnapshot;
  }

  public void setUnitPriceSnapshot(BigDecimal unitPriceSnapshot) {
    this.unitPriceSnapshot = unitPriceSnapshot;
  }

  public BigDecimal getCostPriceSnapshot() {
    return costPriceSnapshot;
  }

  public void setCostPriceSnapshot(BigDecimal costPriceSnapshot) {
    this.costPriceSnapshot = costPriceSnapshot;
  }

  public BigDecimal getVatRateSnapshot() {
    return vatRateSnapshot;
  }

  public void setVatRateSnapshot(BigDecimal vatRateSnapshot) {
    this.vatRateSnapshot = vatRateSnapshot;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getDiscountAmount() {
    return discountAmount;
  }

  public void setDiscountAmount(BigDecimal discountAmount) {
    this.discountAmount = discountAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public void setVatAmount(BigDecimal vatAmount) {
    this.vatAmount = vatAmount;
  }

  public BigDecimal getLineTotal() {
    return lineTotal;
  }

  public void setLineTotal(BigDecimal lineTotal) {
    this.lineTotal = lineTotal;
  }

  public BigDecimal getReturnedQuantity() {
    return returnedQuantity;
  }

  public void setReturnedQuantity(BigDecimal returnedQuantity) {
    this.returnedQuantity = returnedQuantity;
  }
}
