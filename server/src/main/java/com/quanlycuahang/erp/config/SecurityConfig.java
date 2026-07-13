package com.quanlycuahang.erp.config;

import com.quanlycuahang.erp.auth.security.JwtAccessDeniedHandler;
import com.quanlycuahang.erp.auth.security.JwtAuthenticationEntryPoint;
import com.quanlycuahang.erp.auth.security.JwtAuthenticationFilter;
import com.quanlycuahang.erp.auth.security.ResourceActionPermissionEvaluator;
import com.quanlycuahang.erp.auth.security.TenantFilter;
import com.quanlycuahang.erp.common.web.ApiRateLimitFilter;
import com.quanlycuahang.erp.common.web.RequestContextMdcFilter;
import com.quanlycuahang.erp.platform.security.PlatformAdminJwtAuthenticationFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Cau hinh Spring Security cho API JSON thuan (khong session, khong form login). CSRF tat vi API
 * dung Bearer token trong header (khong dua vao session cookie tu dong gui kem moi request nhu form
 * truyen thong) — CSRF chi thuc su can thiet khi trinh duyet tu dong dinh kem credential (cookie)
 * vao request toi domain khac ma nguoi dung khong chu dong; access token nam trong memory JS nen
 * khong bi gui kem tu dong, giam thieu rui ro CSRF o muc chap nhan duoc cho API JSON (D3).
 */
@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  private final JwtAuthenticationFilter jwtAuthenticationFilter;
  private final TenantFilter tenantFilter;
  private final ApiRateLimitFilter apiRateLimitFilter;
  private final RequestContextMdcFilter requestContextMdcFilter;
  private final PlatformAdminJwtAuthenticationFilter platformAdminJwtAuthenticationFilter;
  private final JwtAuthenticationEntryPoint authenticationEntryPoint;
  private final JwtAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      TenantFilter tenantFilter,
      ApiRateLimitFilter apiRateLimitFilter,
      RequestContextMdcFilter requestContextMdcFilter,
      PlatformAdminJwtAuthenticationFilter platformAdminJwtAuthenticationFilter,
      JwtAuthenticationEntryPoint authenticationEntryPoint,
      JwtAccessDeniedHandler accessDeniedHandler) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.tenantFilter = tenantFilter;
    this.apiRateLimitFilter = apiRateLimitFilter;
    this.requestContextMdcFilter = requestContextMdcFilter;
    this.platformAdminJwtAuthenticationFilter = platformAdminJwtAuthenticationFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  /**
   * Chain RIENG cho Super Admin - securityMatcher tach han khoi chain con lai (khop truoc do
   * co @Order thap hon), dung filter/authority rieng (PLATFORM_ADMIN), KHONG dung TenantFilter
   * (Super Admin khong thuoc tenant nao, xem PlatformAdminJwtAuthenticationFilter).
   */
  @Bean
  @Order(1)
  public SecurityFilterChain platformAdminFilterChain(HttpSecurity http) throws Exception {
    http.securityMatcher("/api/v1/platform-admin/**")
        .csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource(null)))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/platform-admin/auth/login", "/api/v1/platform-admin/auth/refresh")
                    .permitAll()
                    .anyRequest()
                    .hasAuthority("PLATFORM_ADMIN"))
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(
            platformAdminJwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(apiRateLimitFilter, PlatformAdminJwtAuthenticationFilter.class);
    return http.build();
  }

  @Bean
  @Order(2)
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource(null)))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        // /actuator/** (khong chi health/info nua - Prompt #8 them metrics/
                        // prometheus): ranh gioi bao mat that su la MANG, khong phai xac thuc o
                        // day - nginx KHONG proxy /actuator ra Internet va port server khong
                        // publish ra host (xem docker/nginx.conf + docker-compose.yml), nen
                        // permitAll o tang ung dung an toan (Prometheus scrape tu container
                        // khac trong docker-compose network khong the/khong nen mang JWT).
                        "/actuator/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    // GET /uploads cong khai la CO CHU DICH (da ra soat): file upload hien chi la
                    // anh QR ngan hang/anh san pham, va anh QR phai hien duoc tren trang tra cuu
                    // hoa don CONG KHAI (khach quet QR, khong dang nhap - xem InvoiceLookupPage).
                    // Ten file la UUID ngau nhien (khong doan duoc). Neu sau nay co loai upload
                    // rieng tu (hop dong, chung tu...) thi KHONG dung chung duong dan nay - phai
                    // them kiem tra dang nhap + so huu tenant rieng.
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/uploads/**")
                    .permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/v1/invoices/lookup/**")
                    .permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/v1/settings/branding")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(tenantFilter, JwtAuthenticationFilter.class)
        .addFilterAfter(requestContextMdcFilter, TenantFilter.class)
        .addFilterAfter(apiRateLimitFilter, RequestContextMdcFilter.class);
    return http.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource(
      @Value("${app.cors.allowed-origins:}") String allowedOrigins) {
    CorsConfiguration configuration = new CorsConfiguration();
    String origins = (allowedOrigins == null || allowedOrigins.isBlank()) ? "" : allowedOrigins;
    configuration.setAllowedOrigins(origins.isBlank() ? List.of() : List.of(origins.split(",")));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setExposedHeaders(List.of("X-Correlation-Id"));
    configuration.setAllowCredentials(true);
    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder(12);
  }

  @Bean
  public DaoAuthenticationProvider authenticationProvider(
      UserDetailsService userDetailsService, PasswordEncoder passwordEncoder) {
    DaoAuthenticationProvider provider = new DaoAuthenticationProvider();
    provider.setUserDetailsService(userDetailsService);
    provider.setPasswordEncoder(passwordEncoder);
    return provider;
  }

  @Bean
  public AuthenticationManager authenticationManager(DaoAuthenticationProvider provider) {
    return new org.springframework.security.authentication.ProviderManager(provider);
  }

  @Bean
  public DefaultMethodSecurityExpressionHandler methodSecurityExpressionHandler(
      ResourceActionPermissionEvaluator permissionEvaluator) {
    DefaultMethodSecurityExpressionHandler handler = new DefaultMethodSecurityExpressionHandler();
    handler.setPermissionEvaluator(permissionEvaluator);
    return handler;
  }

  /**
   * Tat dang ky filter TU DONG cua Spring Boot cho ApiRateLimitFilter - filter nay la @Component
   * (de duoc inject vao day) nen Boot mac dinh tu dang ky them 1 ban nua vao filter chain goc,
   * NGOAI vi tri da gan thu cong trong 2 SecurityFilterChain o tren (sau filter xac thuc, vi can
   * username). Hien khong gay loi CHI VI OncePerRequestFilter tu chan chay lap trong 1 request -
   * tat han o day de khong phu thuoc vao hieu ung phu do (phat hien khi rieng soat).
   */
  @Bean
  public org.springframework.boot.web.servlet.FilterRegistrationBean<ApiRateLimitFilter>
      apiRateLimitFilterRegistration(ApiRateLimitFilter filter) {
    var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }

  /**
   * Tat dang ky filter TU DONG cua Spring Boot cho RequestContextMdcFilter - cung ly do voi
   * ApiRateLimitFilter o tren: filter nay PHAI chay SAU JwtAuthenticationFilter/TenantFilter (can
   * TenantContext/SecurityContext da duoc gan) trong chuoi filter cua Spring Security, khong phai o
   * vi tri Spring Boot tu dong xep (truoc ca springSecurityFilterChain).
   */
  @Bean
  public org.springframework.boot.web.servlet.FilterRegistrationBean<RequestContextMdcFilter>
      requestContextMdcFilterRegistration(RequestContextMdcFilter filter) {
    var registration = new org.springframework.boot.web.servlet.FilterRegistrationBean<>(filter);
    registration.setEnabled(false);
    return registration;
  }
}
