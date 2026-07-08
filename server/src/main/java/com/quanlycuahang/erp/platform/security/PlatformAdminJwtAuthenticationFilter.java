package com.quanlycuahang.erp.platform.security;

import com.quanlycuahang.erp.auth.security.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Rieng cho SecurityFilterChain /api/v1/platform-admin/** — chi chap nhan token mang claim "scope"
 * = platform_admin (sinh boi JwtService.generatePlatformAdminAccessToken), tu choi token cua tenant
 * User du ky dung chung 1 khoa HMAC. Khong dung TenantContext (PlatformAdmin khong thuoc tenant nao).
 */
@Component
public class PlatformAdminJwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer ";
  private static final String AUTHORITY_PLATFORM_ADMIN = "PLATFORM_ADMIN";

  private final JwtService jwtService;

  public PlatformAdminJwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String header = request.getHeader(HEADER);
    if (header != null && header.startsWith(PREFIX)) {
      String token = header.substring(PREFIX.length());
      try {
        Claims claims = jwtService.parseClaims(token);
        String username = jwtService.extractUsername(claims);
        if (username != null
            && jwtService.isPlatformAdminToken(claims)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
          var authToken =
              new UsernamePasswordAuthenticationToken(
                  username, null, List.of(new SimpleGrantedAuthority(AUTHORITY_PLATFORM_ADMIN)));
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
        }
      } catch (JwtException | IllegalArgumentException ex) {
        SecurityContextHolder.clearContext();
      }
    }
    filterChain.doFilter(request, response);
  }
}
