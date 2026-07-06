package com.quanlycuahang.erp.partner.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

@Entity
@Table(name = "suppliers")
@SQLDelete(sql = "UPDATE suppliers SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Supplier extends BaseEntity {

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "phone")
  private String phone;

  @Column(name = "address")
  private String address;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }
}
