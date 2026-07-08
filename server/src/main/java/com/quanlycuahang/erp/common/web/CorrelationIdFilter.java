package com.quanlycuahang.erp.common.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gan correlation id vao MDC cho moi request de log co the truy vet xuyen suot 1 request (D2, yeu
 * cau ky thuat Phan C — logging). Uu tien lay tu header X-Correlation-Id (client/gateway gui
 * xuong), khong co thi tu sinh.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CorrelationIdFilter extends OncePerRequestFilter {

  private static final String HEADER_NAME = "X-Correlation-Id";
  private static final String MDC_KEY = "correlationId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    String correlationId = request.getHeader(HEADER_NAME);
    if (correlationId == null || correlationId.isBlank()) {
      correlationId = UUID.randomUUID().toString();
    }
    try {
      MDC.put(MDC_KEY, correlationId);
      response.setHeader(HEADER_NAME, correlationId);
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_KEY);
    }
  }
}
