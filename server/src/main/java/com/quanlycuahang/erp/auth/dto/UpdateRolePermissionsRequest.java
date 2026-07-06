package com.quanlycuahang.erp.auth.dto;

import jakarta.validation.constraints.NotNull;
import java.util.Set;

public class UpdateRolePermissionsRequest {

  @NotNull private Set<String> permissionCodes;

  public Set<String> getPermissionCodes() {
    return permissionCodes;
  }

  public void setPermissionCodes(Set<String> permissionCodes) {
    this.permissionCodes = permissionCodes;
  }
}
