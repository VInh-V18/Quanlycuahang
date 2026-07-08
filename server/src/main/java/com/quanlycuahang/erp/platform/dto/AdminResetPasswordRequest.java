package com.quanlycuahang.erp.platform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Super Admin dat lai mat khau ho 1 tai khoan tenant (khong can biet mat khau cu - khac
 * ChangePasswordRequest tu doi mat khau cua chinh minh). */
public class AdminResetPasswordRequest {

  @NotBlank
  @Size(min = 8, message = "Mat khau phai co it nhat 8 ky tu")
  private String newPassword;

  public String getNewPassword() {
    return newPassword;
  }

  public void setNewPassword(String newPassword) {
    this.newPassword = newPassword;
  }
}
