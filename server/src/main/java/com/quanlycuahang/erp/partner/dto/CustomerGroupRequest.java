package com.quanlycuahang.erp.partner.dto;

import jakarta.validation.constraints.NotBlank;

public class CustomerGroupRequest {

  @NotBlank private String name;

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }
}
