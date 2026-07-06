package com.quanlycuahang.erp.auth.service;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.auth.security.AuthException;
import com.quanlycuahang.erp.auth.security.AuthTokens;
import com.quanlycuahang.erp.auth.security.JwtService;
import com.quanlycuahang.erp.auth.security.RefreshTokenService;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.web.RateLimitService;
import com.quanlycuahang.erp.system.service.SettingsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Dieu phoi toan bo luong xac thuc: dang nhap (rate limit + xac thuc), refresh token rotation +
 * phat hien reuse, dang xuat, doi mat khau. Khong biet HttpServletRequest/Response — Controller
 * chiu trach nhiem doc/ghi cookie va lay client IP (D3, Phase 2 kien truc).
 */
@Service
public class AuthService {

  private static final Duration LOGIN_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final RateLimitService rateLimitService;
  private final PasswordEncoder passwordEncoder;
  private final SettingsService settingsService;

  public AuthService(
      AuthenticationManager authenticationManager,
      UserDetailsService userDetailsService,
      UserRepository userRepository,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      RateLimitService rateLimitService,
      PasswordEncoder passwordEncoder,
      SettingsService settingsService) {
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.rateLimitService = rateLimitService;
    this.passwordEncoder = passwordEncoder;
    this.settingsService = settingsService;
  }

  public AuthTokens login(String username, String password, String clientIp) {
    int maxAttempts =
        Integer.parseInt(
            settingsService.getValue(null, SettingsService.KEY_LOGIN_RATE_LIMIT_ATTEMPTS, "5"));
    boolean allowed =
        rateLimitService.tryConsume("login:" + clientIp, maxAttempts, LOGIN_RATE_LIMIT_WINDOW);
    if (!allowed) {
      throw AuthException.rateLimitExceeded();
    }

    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(username, password));
    } catch (AuthenticationException ex) {
      throw AuthException.invalidCredentials();
    }

    List<String> authorities = extractAuthorityCodes(authentication.getAuthorities());
    String tokenFamily = UUID.randomUUID().toString();
    return issueTokenPair(username, authorities, tokenFamily);
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
      // Token da rotate qua VA nam ngoai khoang grace -> nghi ngo bi danh cap, thu hoi ca chuoi.
      refreshTokenService.revokeFamily(tokenFamily);
      throw AuthException.refreshTokenReuseDetected();
    }
    // jti dang hop le, hoac nam trong khoang grace (2 refresh gan nhu dong thoi voi cung 1 token
    // hop phap) -> cap token moi binh thuong, KHONG thu hoi chuoi.

    UserDetails userDetails = userDetailsService.loadUserByUsername(username);
    if (!userDetails.isEnabled()) {
      throw AuthException.invalidRefreshToken();
    }
    List<String> authorities = extractAuthorityCodes(userDetails.getAuthorities());
    return issueTokenPair(username, authorities, tokenFamily);
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
    User user =
        userRepository
            .findByUsernameAndActiveTrue(username)
            .orElseThrow(AuthException::invalidCredentials);
    if (!passwordEncoder.matches(oldPassword, user.getPasswordHash())) {
      throw new BusinessRuleException("AUTH_INVALID_OLD_PASSWORD", "Mat khau cu khong dung");
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  private AuthTokens issueTokenPair(String username, List<String> authorities, String tokenFamily) {
    String accessToken = jwtService.generateAccessToken(username, authorities);
    JwtService.GeneratedRefreshToken refresh =
        jwtService.generateRefreshToken(username, tokenFamily);
    refreshTokenService.storeCurrentJti(tokenFamily, refresh.jti(), refresh.ttlMillis());
    return new AuthTokens(accessToken, refresh.token(), refresh.ttlMillis());
  }

  private List<String> extractAuthorityCodes(
      java.util.Collection<? extends GrantedAuthority> authorities) {
    return AuthorityUtils.authorityListToSet(authorities).stream().toList();
  }
}
