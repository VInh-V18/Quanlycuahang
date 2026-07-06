package com.quanlycuahang.erp.partner.dto;

import java.math.BigDecimal;

public class CustomerResponse {

  private Long id;
  private String name;
  private String phone;
  private String address;
  private String email;
  private Long customerGroupId;
  private BigDecimal debtLimit;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
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

  public Long getCustomerGroupId() {
    return customerGroupId;
  }

  public void setCustomerGroupId(Long customerGroupId) {
    this.customerGroupId = customerGroupId;
  }

  public BigDecimal getDebtLimit() {
    return debtLimit;
  }

  public void setDebtLimit(BigDecimal debtLimit) {
    this.debtLimit = debtLimit;
  }
}
