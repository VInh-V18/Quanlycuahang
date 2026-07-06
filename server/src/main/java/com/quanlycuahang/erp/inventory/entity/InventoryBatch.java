package com.quanlycuahang.erp.inventory.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.system.entity.Branch;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Lo hang + HSD ghi nhan tai thoi diem nhap kho (FH-4). Day la thong tin THAM KHAO cho canh bao can
 * het han va hien thi "Lo gan nhat/HSD" o Ton kho — khong phai nguon su that cho ton kho hien tai
 * (inventory.stock van la nguon duy nhat, giu nguyen tinh binh quan gia quyen D5/B4); quantity o
 * day la SL nhap luc do, khong bi tru khi ban/kiem ke.
 */
@Entity
@Table(name = "inventory_batches")
@SQLDelete(sql = "UPDATE inventory_batches SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class InventoryBatch extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_item_id")
  private PurchaseOrderItem purchaseOrderItem;

  @Column(name = "batch_code", nullable = false)
  private String batchCode;

  @Column(name = "expiry_date")
  private LocalDate expiryDate;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "cost_price", nullable = false)
  private BigDecimal costPrice;

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

  public PurchaseOrderItem getPurchaseOrderItem() {
    return purchaseOrderItem;
  }

  public void setPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
    this.purchaseOrderItem = purchaseOrderItem;
  }

  public String getBatchCode() {
    return batchCode;
  }

  public void setBatchCode(String batchCode) {
    this.batchCode = batchCode;
  }

  public LocalDate getExpiryDate() {
    return expiryDate;
  }

  public void setExpiryDate(LocalDate expiryDate) {
    this.expiryDate = expiryDate;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getCostPrice() {
    return costPrice;
  }

  public void setCostPrice(BigDecimal costPrice) {
    this.costPrice = costPrice;
  }
}
