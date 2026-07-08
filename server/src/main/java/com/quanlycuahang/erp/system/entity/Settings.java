package com.quanlycuahang.erp.system.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/** Cau hinh he thong theo chi nhanh (branch = null la cau hinh global). */
@Entity
@Table(name = "settings")
@SQLDelete(sql = "UPDATE settings SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Settings extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "branch_id")
  private Branch branch;

  @Column(name = "key", nullable = false)
  private String key;

  @Column(name = "value")
  private String value;

  public Branch getBranch() {
    return branch;
  }

  public void setBranch(Branch branch) {
    this.branch = branch;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }
}
