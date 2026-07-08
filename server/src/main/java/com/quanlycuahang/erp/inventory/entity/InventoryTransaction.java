package com.quanlycuahang.erp.inventory.entity;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import com.quanlycuahang.erp.product.entity.Product;
import com.quanlycuahang.erp.system.entity.Branch;
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
 * The kho — nguon chan ly duy nhat cho moi bien dong ton (B4): ton hien tai luon tinh lai duoc tu
 * lich su bang nay. type: purchase, sale, supplier_return, customer_return, stock_take, transfer,
 * cancel.
 */
@Entity
@Table(name = "inventory_transactions")
@SQLDelete(sql = "UPDATE inventory_transactions SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class InventoryTransaction extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "product_id", nullable = false)
  private Product product;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "branch_id", nullable = false)
  private Branch branch;

  @Column(name = "type", nullable = false)
  private String type;

  @Column(name = "quantity", nullable = false)
  private BigDecimal quantity;

  @Column(name = "unit_cost")
  private BigDecimal unitCost;

  /** Chi co gia tri voi type=purchase - dung de tim dung dong khi sua gia nhap 1 phieu cu (B4
   * mo rong, xem AverageCostService.replay()). */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "purchase_order_item_id")
  private PurchaseOrderItem purchaseOrderItem;

  @Column(name = "reference_type")
  private String referenceType;

  @Column(name = "reference_id")
  private Long referenceId;

  @Column(name = "note")
  private String note;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "created_by")
  private User createdBy;

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

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public BigDecimal getQuantity() {
    return quantity;
  }

  public void setQuantity(BigDecimal quantity) {
    this.quantity = quantity;
  }

  public BigDecimal getUnitCost() {
    return unitCost;
  }

  public void setUnitCost(BigDecimal unitCost) {
    this.unitCost = unitCost;
  }

  public PurchaseOrderItem getPurchaseOrderItem() {
    return purchaseOrderItem;
  }

  public void setPurchaseOrderItem(PurchaseOrderItem purchaseOrderItem) {
    this.purchaseOrderItem = purchaseOrderItem;
  }

  public String getReferenceType() {
    return referenceType;
  }

  public void setReferenceType(String referenceType) {
    this.referenceType = referenceType;
  }

  public Long getReferenceId() {
    return referenceId;
  }

  public void setReferenceId(Long referenceId) {
    this.referenceId = referenceId;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }

  public User getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(User createdBy) {
    this.createdBy = createdBy;
  }
}
