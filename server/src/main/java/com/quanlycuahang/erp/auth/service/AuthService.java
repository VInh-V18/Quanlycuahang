package com.quanlycuahang.erp.auth.service;

import com.quanlycuahang.erp.auth.entity.User;
import com.quanlycuahang.erp.auth.repository.UserRepository;
import com.quanlycuahang.erp.auth.security.AuthException;
import com.quanlycuahang.erp.auth.security.AuthTokens;
import com.quanlycuahang.erp.auth.security.JwtService;
import com.quanlycuahang.erp.auth.security.RefreshTokenService;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.common.metrics.BusinessMetrics;
import com.quanlycuahang.erp.common.web.RateLimitService;
import com.quanlycuahang.erp.system.service.SettingsService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

  private static final Logger log = LoggerFactory.getLogger(AuthService.class);
  private static final Duration LOGIN_RATE_LIMIT_WINDOW = Duration.ofMinutes(15);

  private final AuthenticationManager authenticationManager;
  private final UserDetailsService userDetailsService;
  private final UserRepository userRepository;
  private final JwtService jwtService;
  private final RefreshTokenService refreshTokenService;
  private final RateLimitService rateLimitService;
  private final PasswordEncoder passwordEncoder;
  private final SettingsService settingsService;
  private final BusinessMetrics businessMetrics;

  public AuthService(
      AuthenticationManager authenticationManager,
      UserDetailsService userDetailsService,
      UserRepository userRepository,
      JwtService jwtService,
      RefreshTokenService refreshTokenService,
      RateLimitService rateLimitService,
      PasswordEncoder passwordEncoder,
      SettingsService settingsService,
      BusinessMetrics businessMetrics) {
    this.authenticationManager = authenticationManager;
    this.userDetailsService = userDetailsService;
    this.userRepository = userRepository;
    this.jwtService = jwtService;
    this.refreshTokenService = refreshTokenService;
    this.rateLimitService = rateLimitService;
    this.passwordEncoder = passwordEncoder;
    this.settingsService = settingsService;
    this.businessMetrics = businessMetrics;
  }

  /**
   * @Transactional: requireTenantId() doc user.getTenant().isActive() qua quan he @ManyToOne LAZY -
   * can Session con mo de Hibernate lazy-load Tenant (khac getTenant().getId() truoc day, luon co
   * san tu FK khong can lazy-load) - thieu annotation nay se nem LazyInitializationException.
   */
  @Transactional(readOnly = true)
  public AuthTokens login(String username, String password, String clientIp) {
    // TenantContext CHUA duoc gan luc nay (request nay chua co JWT) - khong the dung
    // settingsService.getValue(null, ...) thuong (dua vao TenantContext) vi se suy bien thanh 1
    // khoa cache/1 truy van DUNG CHUNG cho MOI tenant (phat hien khi rieng soat bao mat: 1 tenant
    // tuy chinh gioi han dang nhap se anh huong toi tenant khac, hoac lam dang nhap loi toan he
    // thong neu co 2 tenant tro len cung tuy chinh). Tu tra tenantId truoc (khong nem loi neu
    // username sai/khong ton tai - viec do de authenticationManager.authenticate() ben duoi xu ly
    // dung usual, o day chi can 1 gia tri hop ly de ap dung gioi han).
    Long tenantId = resolveTenantIdQuietly(username);
    int maxAttempts =
        tenantId == null
            ? 5
            : Integer.parseInt(
                settingsService.getValueForTenant(
                    tenantId, SettingsService.KEY_LOGIN_RATE_LIMIT_ATTEMPTS, "5"));
    // 2 bucket doc lap: theo IP (chan brute-force xoay IP nham 1 tai khoan) VA theo username (chan
    // brute-force tu 1 IP dung chung/NAT nham nhieu tai khoan khac nhau) - truoc day chi co bucket
    // IP, ke tan cong xoay IP (proxy/botnet) co the do mat khau 1 tai khoan cu the khong gioi han
    // (phat hien khi rieng soat).
    String ipRateLimitKey = "login:" + clientIp;
    String userRateLimitKey = "login:user:" + username.trim().toLowerCase();
    boolean ipAllowed =
        rateLimitService.tryConsume(ipRateLimitKey, maxAttempts, LOGIN_RATE_LIMIT_WINDOW);
    boolean userAllowed =
        rateLimitService.tryConsume(userRateLimitKey, maxAttempts, LOGIN_RATE_LIMIT_WINDOW);
    if (!ipAllowed || !userAllowed) {
      // Su kien nhay cam (Prompt #8, P2 quan sat): dang nhap sai VUOT NGUONG (khac 1 lan sai
      // don le) - dau hieu brute-force, can theo doi rieng khoi AUTH_INVALID_CREDENTIALS thuong.
      log.warn(
          "LOGIN_RATE_LIMIT_EXCEEDED username={} clientIp={} tenantId={}",
          username,
          clientIp,
          tenantId);
      businessMetrics.recordLoginFailed(tenantId, "rate_limited");
      throw AuthException.rateLimitExceeded();
    }

    Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              new UsernamePasswordAuthenticationToken(username, password));
    } catch (AuthenticationException ex) {
      log.warn("LOGIN_FAILED username={} clientIp={} tenantId={}", username, clientIp, tenantId);
      businessMetrics.recordLoginFailed(tenantId, "bad_credentials");
      throw AuthException.invalidCredentials();
    }

    // Dang nhap dung — hoan lai luot vua tru: gioi han nay chi de chan do mat khau (dang nhap
    // SAI nhieu lan), khong nham chan nguoi dung dang nhap dung nhieu lan (nhieu tab, doi ca).
    rateLimitService.refund(ipRateLimitKey, maxAttempts, LOGIN_RATE_LIMIT_WINDOW);
    rateLimitService.refund(userRateLimitKey, maxAttempts, LOGIN_RATE_LIMIT_WINDOW);

    List<String> authorities = extractAuthorityCodes(authentication.getAuthorities());
    // Ghi de bang gia tri da kiem tra day du (bao gom ca tenant.isActive()) - tenantId o tren chi
    // dung tam de doc dung cau hinh gioi han dang nhap, khong thay the cho requireTenantId().
    tenantId = requireTenantId(username);
    String tokenFamily = UUID.randomUUID().toString();
    return issueTokenPair(username, authorities, tenantId, tokenFamily);
  }

  @Transactional(readOnly = true)
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
    Long tenantId = requireTenantId(username);
    return issueTokenPair(username, authorities, tenantId, tokenFamily);
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
      throw new BusinessRuleException("AUTH_INVALID_OLD_PASSWORD", "Mật khẩu cũ không đúng");
    }
    user.setPasswordHash(passwordEncoder.encode(newPassword));
    userRepository.save(user);
  }

  private AuthTokens issueTokenPair(
      String username, List<String> authorities, Long tenantId, String tokenFamily) {
    String accessToken = jwtService.generateAccessToken(username, authorities, tenantId);
    JwtService.GeneratedRefreshToken refresh =
        jwtService.generateRefreshToken(username, tokenFamily);
    refreshTokenService.storeCurrentJti(tokenFamily, refresh.jti(), refresh.ttlMillis());
    return new AuthTokens(accessToken, refresh.token(), refresh.ttlMillis());
  }

  /**
   * User luon thuoc dung 1 tenant (khong nullable) — chi PlatformAdmin (dang nhap rieng, khong qua
   * AuthService nay) moi khong gan tenant nao. Tu choi dang nhap/refresh neu Super Admin da tam
   * khoa tenant nay (Tenant.active=false) - neu khong, tinh nang khoa tenant se khong co tac dung
   * gi thuc te (nguoi dung tenant bi khoa van dang nhap/refresh binh thuong duoc).
   */
  private Long requireTenantId(String username) {
    User user =
        userRepository
            .findByUsernameAndActiveTrue(username)
            .orElseThrow(AuthException::invalidCredentials);
    if (!user.getTenant().isActive()) {
      throw AuthException.tenantDisabled();
    }
    return user.getTenant().getId();
  }

  /**
   * Nhu requireTenantId() nhung KHONG nem loi neu khong tim thay/khong hop le - dung o buoc doc
   * gioi han dang nhap TRUOC khi xac thuc mat khau, luc chua the/chua nen bao "sai tai khoan".
   */
  private Long resolveTenantIdQuietly(String username) {
    return userRepository
        .findByUsernameAndActiveTrue(username)
        .map(user -> user.getTenant().getId())
        .orElse(null);
  }

  private List<String> extractAuthorityCodes(
      java.util.Collection<? extends GrantedAuthority> authorities) {
    return AuthorityUtils.authorityListToSet(authorities).stream().toList();
  }
}
