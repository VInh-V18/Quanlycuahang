package com.quanlycuahang.erp.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiError;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.metrics.BusinessMetrics;
import com.quanlycuahang.erp.system.service.SettingsService;
import io.github.bucket4j.ConsumptionProbe;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit chung cho API, phan tang theo nhom endpoint (Prompt #3 — bo prompt nang cap):
 *
 * <p><b>Dieu tra "rollback cu"</b>: roadmap gia dinh filter nay tung bi TAT sau 1 lan rollback —
 * kiem tra thuc te bang {@code git log --all -- ApiRateLimitFilter.java} chi thay 1 commit DUY NHAT
 * ("Initial commit"), KHONG co bat ky rollback/revert nao trong lich su repo nay; filter da duoc
 * BAT san (dang ky trong SecurityConfig) tu dau — gia dinh cua roadmap KHONG khop voi repo that (co
 * the la template chung, khong danh rieng cho codebase nay). Tuy nhien ban cu (mac phang 100
 * req/phut/user cho MOI API, khong phan biet nhom, KHONG fail-open khi Redis loi) co dung 1 lo hong
 * that: {@code rateLimitService.tryConsume()} goi thang Lettuce/Redis dong bo, KHONG try/catch —
 * Redis timeout/mat ket noi se nem RuntimeException chua bat, lam MOI request da dang nhap tra loi
 * 500 (chan toan he thong ban hang vi ha tang phu, dung dieu roadmap canh bao). Da vá tai day.
 *
 * <p>Thiet ke moi: khoa theo username (da la duy nhat toan he thong, khong can ghep tenantId de
 * tranh dung chung bucket giua 2 nguoi dung — xem TestDataFactory/username generation), phan tang
 * theo {@link RateLimitTier}, fail-open co kiem soat khi Redis loi (log WARN, cho qua), va co
 * "rate_limit_mode" (settings, tat nhanh khong can redeploy: off/shadow/enforce).
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);

  private final RateLimitService rateLimitService;
  private final SettingsService settingsService;
  private final ObjectMapper objectMapper;
  private final BusinessMetrics businessMetrics;

  public ApiRateLimitFilter(
      RateLimitService rateLimitService,
      SettingsService settingsService,
      ObjectMapper objectMapper,
      BusinessMetrics businessMetrics) {
    this.rateLimitService = rateLimitService;
    this.settingsService = settingsService;
    this.objectMapper = objectMapper;
    this.businessMetrics = businessMetrics;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      filterAuthenticated(request, response, filterChain, authentication);
      return;
    }
    // Endpoint cong khai tra cuu hoa don (khong dang nhap, xem SecurityConfig permitAll) — chong
    // do quet lookupCode bang IP, LUON enforce (khong theo "rate_limit_mode" vi khong co
    // TenantContext o day de tra cai dat theo tung tenant, va day la bao ve toan he thong chu
    // khong phai tuy chon nghiep vu tung cua hang).
    if (RateLimitTier.isPublicLookupEndpoint(request)) {
      filterPublicLookup(request, response, filterChain);
      return;
    }
    filterChain.doFilter(request, response);
  }

  private static final String AUTHORITY_PLATFORM_ADMIN = "PLATFORM_ADMIN";

  private void filterAuthenticated(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain,
      Authentication authentication)
      throws ServletException, IOException {
    // PlatformAdmin KHONG thuoc tenant nao (khong bind TenantContext — xem
    // PlatformAdminJwtAuthenticationFilter), nen KHONG duoc goi SettingsService (tenant-scoped) o
    // day: voi TenantContext null, truy van "rate_limit_mode" toan cuc se KHONG loc duoc theo
    // tenant qua Hibernate @Filter, va se NEM LOI ngay khi co >=2 tenant cung tuy chinh khoa nay
    // (Spring Data nem IncorrectResultSizeDataAccessException do tra ve nhieu dong) — se lam SAP
    // TOAN BO API cua Super Admin. PlatformAdmin luon "enforce" co dinh, khong co "tat nhanh qua
    // settings" (dung luong thap, chi Chu he thong dung, khong co ly do nghiep vu de tat).
    boolean isPlatformAdmin =
        authentication.getAuthorities().stream()
            .anyMatch(a -> AUTHORITY_PLATFORM_ADMIN.equals(a.getAuthority()));
    String mode =
        isPlatformAdmin
            ? "enforce"
            : settingsService.getValue(null, SettingsService.KEY_RATE_LIMIT_MODE, "enforce");
    if ("off".equals(mode)) {
      filterChain.doFilter(request, response);
      return;
    }

    RateLimitTier tier = RateLimitTier.classifyAuthenticated(request);
    String key = "api:" + tier.name() + ":" + authentication.getName();
    Optional<ConsumptionProbe> probe = tryConsumeFailOpen(key, tier);
    if (probe.isEmpty() || probe.get().isConsumed()) {
      filterChain.doFilter(request, response);
      return;
    }

    if ("shadow".equals(mode)) {
      // Giai doan 1 (shadow mode, roadmap buoc 3): chi do + log request LE RA bi chan, KHONG
      // chan that — dung de kiem tra khong co false positive truoc khi bat enforce that.
      log.warn(
          "RATE_LIMIT_SHADOW_WOULD_BLOCK key={} tier={} method={} path={}",
          key,
          tier,
          request.getMethod(),
          request.getRequestURI());
      filterChain.doFilter(request, response);
      return;
    }

    // Chan THAT SU (enforce mode) - su kien nhay cam (Prompt #8, P2 quan sat): can biet ai/tier
    // nao dang bi chan de phan biet brute-force that voi 1 user/tich hop dang goi API qua tay.
    log.warn(
        "RATE_LIMIT_BLOCKED key={} tier={} method={} path={}",
        key,
        tier,
        request.getMethod(),
        request.getRequestURI());
    businessMetrics.recordRateLimitRejected(TenantContext.get(), tier.name());
    respondTooManyRequests(response, probe.get());
  }

  private void filterPublicLookup(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String clientIp = ClientIpResolver.resolve(request);
    String key = "api:" + RateLimitTier.PUBLIC_LOOKUP.name() + ":" + clientIp;
    Optional<ConsumptionProbe> probe = tryConsumeFailOpen(key, RateLimitTier.PUBLIC_LOOKUP);
    if (probe.isEmpty() || probe.get().isConsumed()) {
      filterChain.doFilter(request, response);
      return;
    }
    log.warn(
        "RATE_LIMIT_BLOCKED key={} tier={} method={} path={}",
        key,
        RateLimitTier.PUBLIC_LOOKUP,
        request.getMethod(),
        request.getRequestURI());
    businessMetrics.recordRateLimitRejected(null, RateLimitTier.PUBLIC_LOOKUP.name());
    respondTooManyRequests(response, probe.get());
  }

  /**
   * Fail-open co kiem soat (roadmap buoc 2): Redis chet -> log WARN + cho request di qua, KHONG
   * chan toan he thong vi ha tang rate-limit (day la nguyen nhan that gay loi 500 toan he thong o
   * ban cu neu Redis gian doan — xem Javadoc class).
   */
  private Optional<ConsumptionProbe> tryConsumeFailOpen(String key, RateLimitTier tier) {
    try {
      return Optional.of(rateLimitService.consume(key, tier.capacity(), tier.period()));
    } catch (RuntimeException ex) {
      log.warn(
          "RATE_LIMIT_FAIL_OPEN key={} tier={} - Redis loi, cho request di qua", key, tier, ex);
      return Optional.empty();
    }
  }

  private void respondTooManyRequests(HttpServletResponse response, ConsumptionProbe probe)
      throws IOException {
    long retryAfterSeconds =
        Math.max(1, Duration.ofNanos(probe.getNanosToWaitForRefill()).toSeconds() + 1);
    response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
    response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(retryAfterSeconds));
    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
    ApiError error =
        new ApiError("RATE_LIMITED", "Quá nhiều yêu cầu, vui lòng thử lại sau ít phút", Map.of());
    objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
  }
}
