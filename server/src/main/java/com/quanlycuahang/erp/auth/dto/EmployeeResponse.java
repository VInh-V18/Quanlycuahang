package com.quanlycuahang.erp.auth.dto;

import java.util.List;

public class EmployeeResponse {

  private Long id;
  private String username;
  private String fullName;
  private String phone;
  private boolean active;
  private List<RoleSummaryResponse> roles;

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public String getUsername() {
    return username;
  }

  public void setUsername(String username) {
    this.username = username;
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

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public List<RoleSummaryResponse> getRoles() {
    return roles;
  }

  public void setRoles(List<RoleSummaryResponse> roles) {
    this.roles = roles;
  }
}
