package com.quanlycuahang.erp.auth.security;

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
 * Doc header Authorization: Bearer, dat SecurityContext tu claims trong access token (khong query
 * lai DB moi request — authorities da nam san trong token luc dang nhap/refresh).
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final String HEADER = "Authorization";
  private static final String PREFIX = "Bearer ";

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
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
        List<String> authorities = jwtService.extractAuthorities(claims);
        // Token cua PlatformAdmin (Super Admin) mang claim "scope" rieng, khong co "tenantId" hay
        // "authorities" cua tenant User nao - tu choi ngay o day (ngoai viec da tach URL prefix
        // rieng o SecurityConfig) de token do khong the dung "chay nham" sang API cua 1 cua hang;
        // coi nhu chua dang nhap (khong set Authentication) va de nguyen filter chain chay tiep
        // xuong doan finally chung ben duoi.
        if (username != null
            && !jwtService.isPlatformAdminToken(claims)
            && SecurityContextHolder.getContext().getAuthentication() == null) {
          var authToken =
              new UsernamePasswordAuthenticationToken(
                  username,
                  null,
                  authorities == null
                      ? List.of()
                      : authorities.stream().map(SimpleGrantedAuthority::new).toList());
          authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
          SecurityContextHolder.getContext().setAuthentication(authToken);
          // TenantFilter (chay sau filter nay) doc gia tri nay de bat Hibernate @Filter theo dung
          // tenant cua nguoi dang nhap — PHAI xoa o finally, khong thread pool tai su dung se lam
          // request sau ke thua nham tenantId cua request nay.
          TenantContext.set(jwtService.extractTenantId(claims));
        }
      } catch (JwtException | IllegalArgumentException ex) {
        // Token khong hop le/het han -> khong set Authentication, de
        // AuthenticationEntryPoint tra 401 neu endpoint yeu cau dang nhap.
        SecurityContextHolder.clearContext();
      }
    }
    try {
      filterChain.doFilter(request, response);
    } finally {
      TenantContext.clear();
    }
  }
}
