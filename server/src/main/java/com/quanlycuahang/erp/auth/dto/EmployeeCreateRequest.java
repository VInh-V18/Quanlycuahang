package com.quanlycuahang.erp.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.Set;

public class EmployeeCreateRequest {

  @NotBlank private String username;

  @NotBlank
  @Size(min = 8, message = "Mat khau phai co it nhat 8 ky tu")
  private String password;

  @NotBlank private String fullName;

  private String phone;

  @NotEmpty(message = "Phai chon it nhat 1 vai tro")
  private Set<Long> roleIds;

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
  }

  public String getPassword() {
    return password;
  }

  public void setPassword(String password) {
    this.password = password;
  }

  public String getFullName() {
    return fullName;
  }

  public void setFullName(String fullName) {
    this.fullName = fullName;
  }

  public String getPhone() {
    return phone;
  }

  public void setPhone(String phone) {
    this.phone = phone;
  }

  public Set<Long> getRoleIds() {
    return roleIds;
  }

  public void setRoleIds(Set<Long> roleIds) {
    this.roleIds = roleIds;
  }
}
