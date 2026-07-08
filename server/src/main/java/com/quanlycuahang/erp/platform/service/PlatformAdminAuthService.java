package com.quanlycuahang.erp.platform.service;

import com.quanlycuahang.erp.auth.security.AuthException;
import com.quanlycuahang.erp.auth.security.AuthTokens;
import com.quanlycuahang.erp.auth.security.JwtService;
import com.quanlycuahang.erp.auth.security.RefreshTokenService;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.web.RateLimitService;
import com.quanlycuahang.erp.platform.entity.PlatformAdmin;
import com.quanlycuahang.erp.platform.repository.PlatformAdminRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dang nhap/refresh/dang xuat/doi mat khau rieng cho PlatformAdmin (Super Admin) - CO CHU DINH
 * khong di qua AuthenticationManager/UserDetailsService chung (chi doc bang users) de tranh moi
 * kha nang nham lan voi tai khoan User thong thuong cua 1 tenant; tu kiem tra mat khau truc tiep.
 * RefreshTokenService (Redis, tokenFamily -> jti) dung chung voi tenant User vi ban than co che
 * rotation khong phu thuoc "thuoc ve ai" - tokenFamily la UUID doc lap moi lan dang nhap.
 */
@Service
public class PlatformAdminAuthService {

  private static final Duration LOGIN_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);
  private static final int LOGIN_MAX_ATTEMPTS = 5;

  private final PlatformAdminRepository platformAdminRepository;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final RateLimitService rateLimitService;
  private final PasswordEncoder passwordEncoder;

  public PlatformAdminAuthService(
      PlatformAdminRepository platformAdminRepository,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      RateLimitService rateLimitService,
      PasswordEncoder passwordEncoder) {
    this.platformAdminRepository = platformAdminRepository;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.rateLimitService = rateLimitService;
    this.passwordEncoder = passwordEncoder;
  }

  public AuthTokens login(String username, String password, String clientIp) {
    String rateLimitKey = "platform-admin-login:" + clientIp;
    boolean allowed =
        rateLimitService.tryConsume(rateLimitKey, LOGIN_MAX_ATTEMPTS, LOGIN_RATE_LIMIT_WINDOW);
    if (!allowed) {
      throw AuthException.rateLimitExceeded();
    }

    PlatformAdmin admin = platformAdminRepository.findByUsernameAndActiveTrue(username).orElse(null);
    if (admin == null || !passwordEncoder.matches(password, admin.getPasswordHash())) {
      throw AuthException.invalidCredentials();
    }
    rateLimitService.refund(rateLimitKey, LOGIN_MAX_ATTEMPTS, LOGIN_RATE_LIMIT_WINDOW);

    String tokenFamily = UUID.randomUUID().toString();
    return issueTokenPair(username, tokenFamily);
  }

  public AuthTokens refresh(String refreshToken) {
    Claims claims;
    try {
      claims = jwtService.parseClaims(refreshToken);
    } catch (JwtException | IllegalArgumentException ex) {
      throw AuthException.invalidRefreshToken();
    }

    String username = jwtService.extractUsername(claims);
    String tokenFamily = jwtService.extractTokenFamily(claims);
    String jti = jwtService.extractJti(claims);
    if (username == null || tokenFamily == null || jti == null) {
      throw AuthException.invalidRefreshToken();
    }

    String storedJti = refreshTokenService.getCurrentJti(tokenFamily).orElse(null);
    if (storedJti == null) {
      throw AuthException.invalidRefreshToken();
    }
    if (!storedJti.equals(jti) && !refreshTokenService.isWithinGracePeriod(tokenFamily, jti)) {
      refreshTokenService.revokeFamily(tokenFamily);
      throw AuthException.refreshTokenReuseDetected();
    }

    boolean stillActive =
        platformAdminRepository.findByUsernameAndActiveTrue(username).isPresent();
    if (!stillActive) {
      throw AuthException.invalidRefreshToken();
    }
    return issueTokenPair(username, tokenFamily);
  }

  public void logout(String refreshToken) {
    try {
      Claims claims = jwtService.parseClaims(refreshToken);
      String tokenFamily = jwtService.extractTokenFamily(claims);
      if (tokenFamily != null) {
        refreshTokenService.revokeFamily(tokenFamily);
      }
    } catch (JwtException | IllegalArgumentException ex) {
      // Token da khong hop le san, khong can thu hoi gi them.
    }
  }

  @Transactional
  public void changePassword(String username, String oldPassword, String newPassword) {
    PlatformAdmin admin =
        platformAdminRepository
            .findByUsernameAndActiveTrue(username)
            .orElseThrow(AuthException::invalidCredentials);
    if (!passwordEncoder.matches(oldPassword, admin.getPasswordHash())) {
      throw new BusinessRuleException("AUTH_INVALID_OLD_PASSWORD", "Mat khau cu khong dung");
    }
    admin.setPasswordHash(passwordEncoder.encode(newPassword));
    platformAdminRepository.save(admin);
  }

  private AuthTokens issueTokenPair(String username, String tokenFamily) {
    String accessToken = jwtService.generatePlatformAdminAccessToken(username);
    JwtService.GeneratedRefreshToken refresh =
        jwtService.generateRefreshToken(username, tokenFamily);
    refreshTokenService.storeCurrentJti(tokenFamily, refresh.jti(), refresh.ttlMillis());
    return new AuthTokens(accessToken, refresh.token(), refresh.ttlMillis());
  }
}
