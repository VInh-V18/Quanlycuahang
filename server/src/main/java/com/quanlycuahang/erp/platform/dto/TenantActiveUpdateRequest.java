package com.quanlycuahang.erp.platform.dto;

import jakarta.validation.constraints.NotNull;

public class TenantActiveUpdateRequest {

  @NotNull private Boolean active;

  public Boolean getActive() {
    return active;
  }

  public void setActive(Boolean active) {
    this.active = active;
  }
}
