package com.quanlycuahang.erp.system.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * 1 doanh nghiep/cua hang dang ky tren he thong (multi-tenant, kieu KiotViet) — nam TREN Branch: 1
 * Tenant co the co nhieu Branch (chi nhanh), moi du lieu nghiep vu (san pham, don hang...) deu gan
 * tenant_id truc tiep de dam bao cach ly tuyet doi giua cac cua hang khac nhau.
 */
@Entity
@Table(name = "tenants")
@SQLDelete(sql = "UPDATE tenants SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Tenant extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
