package com.quanlycuahang.erp.partner.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "customer_groups")
@SQLDelete(sql = "UPDATE customer_groups SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class CustomerGroup extends TenantScopedEntity {

  @Column(name = "name", nullable = false)
  private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
