package com.quanlycuahang.erp.auth.security;

import com.quanlycuahang.erp.common.exception.AppException;
import org.springframework.http.HttpStatus;

/**
 * Loi rieng luong xac thuc (401) — sai mat khau, rate limit, refresh token khong hop le/tai su
 * dung.
 */
public class AuthException extends AppException {

  private AuthException(String code, HttpStatus httpStatus, String message) {
    super(code, httpStatus, message);
  }

  public static AuthException invalidCredentials() {
    return new AuthException(
        "AUTH_INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED, "Sai ten dang nhap hoac mat khau");
  }

  public static AuthException rateLimitExceeded() {
    return new AuthException(
        "AUTH_RATE_LIMIT_EXCEEDED",
        HttpStatus.TOO_MANY_REQUESTS,
        "Qua nhieu lan dang nhap that bai, vui long thu lai sau");
  }

  public static AuthException invalidRefreshToken() {
    return new AuthException(
        "AUTH_INVALID_REFRESH_TOKEN",
        HttpStatus.UNAUTHORIZED,
        "Phien dang nhap khong hop le, vui long dang nhap lai");
  }

  public static AuthException refreshTokenReuseDetected() {
    return new AuthException(
        "AUTH_TOKEN_REUSE_DETECTED",
        HttpStatus.UNAUTHORIZED,
        "Phat hien bat thuong ve phien dang nhap, vui long dang nhap lai");
  }

  /**
   * Tenant (cua hang) da bi Super Admin tam khoa - khac AUTH_INVALID_CREDENTIALS (sai mat khau) vi
   * day la trang thai tai khoan/hop dong, khong phai gia doan doan mat khau; noi ro cho chu cua
   * hang biet huong xu ly (lien he quan tri) thay vi de ho tuong minh go sai mat khau.
   */
  public static AuthException tenantDisabled() {
    return new AuthException(
        "AUTH_TENANT_DISABLED",
        HttpStatus.FORBIDDEN,
        "Cua hang cua ban da bi tam khoa, vui long lien he quan tri he thong");
  }
}
