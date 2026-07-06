package com.quanlycuahang.erp.auth.controller;

import com.quanlycuahang.erp.auth.dto.AccessTokenResponse;
import com.quanlycuahang.erp.auth.dto.ChangePasswordRequest;
import com.quanlycuahang.erp.auth.dto.LoginRequest;
import com.quanlycuahang.erp.auth.security.AuthException;
import com.quanlycuahang.erp.auth.security.AuthTokens;
import com.quanlycuahang.erp.auth.service.AuthService;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Dang nhap/refresh/dang xuat/doi mat khau. Refresh token luon di qua httpOnly cookie
 * SameSite=Strict (D3) — khong bao gio xuat hien trong body JSON de tranh XSS doc duoc.
 */
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

  private static final String REFRESH_COOKIE_NAME = "refreshToken";
  private static final String REFRESH_COOKIE_PATH = "/api/v1/auth";

  private final AuthService authService;
  private final boolean refreshCookieSecure;

  public AuthController(
      AuthService authService,
      @Value("${app.auth.refresh-cookie-secure:true}") boolean refreshCookieSecure) {
    this.authService = authService;
    this.refreshCookieSecure = refreshCookieSecure;
  }

  @PostMapping("/login")
  public ResponseEntity<ApiResponse<AccessTokenResponse>> login(
      @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
    AuthTokens tokens =
        authService.login(request.getUsername(), request.getPassword(), clientIp(httpRequest));
    return withRefreshCookie(tokens);
  }

  @PostMapping("/refresh")
  public ResponseEntity<ApiResponse<AccessTokenResponse>> refresh(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
    if (refreshToken == null) {
      throw AuthException.invalidRefreshToken();
    }
    AuthTokens tokens = authService.refresh(refreshToken);
    return withRefreshCookie(tokens);
  }

  @PostMapping("/logout")
  public ResponseEntity<ApiResponse<Void>> logout(
      @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken) {
    if (refreshToken != null) {
      authService.logout(refreshToken);
    }
    ResponseCookie clearCookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .sameSite("Strict")
            .path(REFRESH_COOKIE_PATH)
            .maxAge(0)
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, clearCookie.toString())
        .body(ApiResponse.success(null));
  }

  @PostMapping("/change-password")
  public ResponseEntity<ApiResponse<Void>> changePassword(
      @Valid @RequestBody ChangePasswordRequest request, Authentication authentication) {
    authService.changePassword(
        authentication.getName(), request.getOldPassword(), request.getNewPassword());
    return ResponseEntity.ok(ApiResponse.success(null));
  }

  private ResponseEntity<ApiResponse<AccessTokenResponse>> withRefreshCookie(AuthTokens tokens) {
    ResponseCookie cookie =
        ResponseCookie.from(REFRESH_COOKIE_NAME, tokens.refreshToken())
            .httpOnly(true)
            .secure(refreshCookieSecure)
            .sameSite("Strict")
            .path(REFRESH_COOKIE_PATH)
            .maxAge(Duration.ofMillis(tokens.refreshTokenTtlMillis()))
            .build();
    return ResponseEntity.ok()
        .header(HttpHeaders.SET_COOKIE, cookie.toString())
        .body(ApiResponse.success(new AccessTokenResponse(tokens.accessToken())));
  }

  private String clientIp(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      return forwardedFor.split(",")[0].trim();
    }
    return request.getRemoteAddr();
  }
}
