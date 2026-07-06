package com.quanlycuahang.erp.partner.dto;

import com.quanlycuahang.erp.common.validation.ValidPhoneVN;
import jakarta.validation.constraints.NotBlank;

public class SupplierRequest {

  @NotBlank private String name;

  @ValidPhoneVN private String phone;

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
