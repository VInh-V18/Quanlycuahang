package com.quanlycuahang.erp.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Super Admin tao 1 tenant (cua hang) moi kem tai khoan chu cua hang dau tien (role owner). */
public class TenantCreateRequest {

  @NotBlank private String tenantName;

  private String branchName;

  @NotBlank private String ownerUsername;

  @NotBlank
  @Size(min = 8, message = "Mat khau phai co it nhat 8 ky tu")
  private String ownerPassword;

  @NotBlank private String ownerFullName;

  public String getTenantName() {
    return tenantName;
  }

  public void setTenantName(String tenantName) {
    this.tenantName = tenantName;
  }

  public String getBranchName() {
    return branchName;
  }

  public void setBranchName(String branchName) {
    this.branchName = branchName;
  }

  public String getOwnerUsername() {
    return ownerUsername;
  }

  public void setOwnerUsername(String ownerUsername) {
    this.ownerUsername = ownerUsername;
  }

  public String getOwnerPassword() {
    return ownerPassword;
  }

  public void setOwnerPassword(String ownerPassword) {
    this.ownerPassword = ownerPassword;
  }

  public String getOwnerFullName() {
    return ownerFullName;
  }

  public void setOwnerFullName(String ownerFullName) {
    this.ownerFullName = ownerFullName;
  }
}
