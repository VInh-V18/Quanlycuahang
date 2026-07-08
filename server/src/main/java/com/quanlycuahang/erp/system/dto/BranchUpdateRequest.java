package com.quanlycuahang.erp.system.dto;

import jakarta.validation.constraints.NotBlank;

public class BranchUpdateRequest {

  @NotBlank private String name;

  private String address;

  private String phone;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getAddress() {
    return address;
  }

  public void setAddress(String address) {
    this.address = address;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }
}
