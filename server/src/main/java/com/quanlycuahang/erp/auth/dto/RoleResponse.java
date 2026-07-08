package com.quanlycuahang.erp.auth.dto;

import java.util.Set;

public class RoleResponse {

  private Long id;
  private String code;
  private String displayName;
  private Set<String> permissionCodes;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDisplayName() {
    return displayName;
  }

  public void setDisplayName(String displayName) {
    this.displayName = displayName;
  }

  public Set<String> getPermissionCodes() {
    return permissionCodes;
  }

  public void setPermissionCodes(Set<String> permissionCodes) {
    this.permissionCodes = permissionCodes;
  }
}
