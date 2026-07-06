package com.quanlycuahang.erp.config;

import com.quanlycuahang.erp.auth.security.JwtAccessDeniedHandler;
import com.quanlycuahang.erp.auth.security.JwtAuthenticationEntryPoint;
import com.quanlycuahang.erp.auth.security.JwtAuthenticationFilter;
import com.quanlycuahang.erp.auth.security.ResourceActionPermissionEvaluator;
import com.quanlycuahang.erp.common.web.ApiRateLimitFilter;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
  private final ApiRateLimitFilter apiRateLimitFilter;
  private final JwtAuthenticationEntryPoint authenticationEntryPoint;
  private final JwtAccessDeniedHandler accessDeniedHandler;

  public SecurityConfig(
      JwtAuthenticationFilter jwtAuthenticationFilter,
      ApiRateLimitFilter apiRateLimitFilter,
      JwtAuthenticationEntryPoint authenticationEntryPoint,
      JwtAccessDeniedHandler accessDeniedHandler) {
    this.jwtAuthenticationFilter = jwtAuthenticationFilter;
    this.apiRateLimitFilter = apiRateLimitFilter;
    this.authenticationEntryPoint = authenticationEntryPoint;
    this.accessDeniedHandler = accessDeniedHandler;
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    http.csrf(AbstractHttpConfigurer::disable)
        .cors(cors -> cors.configurationSource(corsConfigurationSource(null)))
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/api/v1/auth/login",
                        "/api/v1/auth/refresh",
                        "/actuator/health",
                        "/actuator/info",
                        "/swagger-ui/**",
                        "/v3/api-docs/**")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/v1/uploads/**")
                    .permitAll()
                    .requestMatchers(
                        org.springframework.http.HttpMethod.GET, "/api/v1/invoices/lookup/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            eh ->
                eh.authenticationEntryPoint(authenticationEntryPoint)
                    .accessDeniedHandler(accessDeniedHandler))
        .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
        .addFilterAfter(apiRateLimitFilter, JwtAuthenticationFilter.class);
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
}
