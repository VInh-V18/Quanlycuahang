package com.quanlycuahang.erp.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quanlycuahang.erp.common.dto.ApiError;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rate limit chung cho API (100 req/phut/user) — truoc day RateLimitService/RateLimitConfig da
 * document "dung cho dang nhap va API chung" nhung chi thuc su duoc goi tu AuthService (dang nhap),
 * phat hien khi rieng soat: toan bo API con lai khong co gioi han tan suat nao. Chay sau
 * JwtAuthenticationFilter (can biet duoc username da xac thuc).
 */
@Component
public class ApiRateLimitFilter extends OncePerRequestFilter {

  private static final long CAPACITY = 100;
  private static final Duration PERIOD = Duration.ofMinutes(1);

  private final RateLimitService rateLimitService;
  private final ObjectMapper objectMapper;

  public ApiRateLimitFilter(RateLimitService rateLimitService, ObjectMapper objectMapper) {
    this.rateLimitService = rateLimitService;
    this.objectMapper = objectMapper;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication != null && authentication.isAuthenticated()) {
      boolean allowed =
          rateLimitService.tryConsume("api:" + authentication.getName(), CAPACITY, PERIOD);
      if (!allowed) {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        ApiError error =
            new ApiError(
                "RATE_LIMITED", "Qua nhieu yeu cau, vui long thu lai sau it phut", Map.of());
        objectMapper.writeValue(response.getWriter(), ApiResponse.error(error));
        return;
      }
    }
    filterChain.doFilter(request, response);
  }
}
