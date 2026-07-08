package com.quanlycuahang.erp.platform.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * Tai khoan quan tri TOAN HE THONG (tao/khoa Tenant) — co chu dinh TACH BIET HOAN TOAN khoi
 * users/Tenant: khong gan tenant_id, khong dung chung JWT/endpoint dang nhap voi nguoi dung trong 1
 * cua hang cu the, tranh moi nham lan giua "quyen van hanh he thong" va "quyen trong 1 tenant".
 */
@Entity
@Table(name = "platform_admins")
@SQLDelete(sql = "UPDATE platform_admins SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class PlatformAdmin extends BaseEntity {

  @Column(name = "username", nullable = false, unique = true)
  private String username;

  @Column(name = "password_hash", nullable = false)
  private String passwordHash;

  @Column(name = "full_name", nullable = false)
  private String fullName;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPasswordHash() {
    return passwordHash;
  }

  public void setPasswordHash(String passwordHash) {
    this.passwordHash = passwordHash;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
