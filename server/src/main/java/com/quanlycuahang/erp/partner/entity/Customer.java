package com.quanlycuahang.erp.partner.entity;

import com.quanlycuahang.erp.common.entity.TenantScopedEntity;
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
@Table(name = "customers")
@SQLDelete(sql = "UPDATE customers SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Customer extends TenantScopedEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "customer_group_id")
  private CustomerGroup customerGroup;

  @Column(name = "name", nullable = false)
  private String name;

  @Column(name = "phone", unique = true)
  private String phone;

  @Column(name = "address")
  private String address;

  @Column(name = "email")
  private String email;

  @Column(name = "debt_limit", nullable = false)
  private BigDecimal debtLimit;

  public CustomerGroup getCustomerGroup() {
    return customerGroup;
  }

  public void setCustomerGroup(CustomerGroup customerGroup) {
    this.customerGroup = customerGroup;
  }

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

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public BigDecimal getDebtLimit() {
    return debtLimit;
  }

  public void setDebtLimit(BigDecimal debtLimit) {
    this.debtLimit = debtLimit;
  }
}
