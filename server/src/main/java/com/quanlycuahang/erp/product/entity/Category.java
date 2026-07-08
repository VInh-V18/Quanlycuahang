package com.quanlycuahang.erp.product.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/** Danh muc cay 2 cap (self-referencing parent) — B4/Phase 1 UC-02. */
@Entity
@Table(name = "categories")
@SQLDelete(sql = "UPDATE categories SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Category extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "parent_id")
  private Category parent;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "display_order", nullable = false)
  private int displayOrder;

  public Category getParent() {
    return parent;
  }

  public void setParent(Category parent) {
    this.parent = parent;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getDisplayOrder() {
    return displayOrder;
  }

  public void setDisplayOrder(int displayOrder) {
    this.displayOrder = displayOrder;
  }
}
