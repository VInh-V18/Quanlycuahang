package com.quanlycuahang.erp.common.web;

import com.quanlycuahang.erp.auth.security.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Gan tenantId/userId/branchId vao MDC (Prompt #8, P2 quan sat) — chay SAU JwtAuthenticationFilter
 * + TenantFilter (dang ky qua SecurityConfig.addFilterAfter(..., TenantFilter.class)) de
 * TenantContext/SecurityContext da san sang luc doc. Ket hop voi correlationId
 * (CorrelationIdFilter, da co tu truoc, dong vai tro "requestId" cua roadmap qua header
 * X-Correlation-Id) cho MOI dong log nghiep vu trong request co du 4 truong de loc theo dung tenant
 * khi 1 cua hang bao loi.
 *
 * <p>userId ghi bang USERNAME (khong phai id so DB) - lay thang tu SecurityContext (da co san tu
 * JWT claims), tranh 1 truy van DB moi request chi de phuc vu logging. branchId chi lay duoc khi
 * request truyen qua query param "branchId" (da la quy uoc pho bien o cac Controller GET danh sach)
 * - request voi branchId nam trong JSON body (POST/PUT) se KHONG co branchId trong MDC, vi doc body
 * o day se tieu thu stream truoc Controller (pham vi da ghi ro, chap nhan duoc vi log dua tren
 * query param da bao phu phan lon luong doc/liet ke can debug nhat).
 */
@Component
public class RequestContextMdcFilter extends OncePerRequestFilter {

  private static final String MDC_TENANT_ID = "tenantId";
  private static final String MDC_USER_ID = "userId";
  private static final String MDC_BRANCH_ID = "branchId";

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    try {
      Long tenantId = TenantContext.get();
      if (tenantId != null) {
        MDC.put(MDC_TENANT_ID, String.valueOf(tenantId));
      }
      Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
      if (authentication != null && authentication.getName() != null) {
        MDC.put(MDC_USER_ID, authentication.getName());
      }
      String branchId = request.getParameter("branchId");
      if (branchId != null && !branchId.isBlank()) {
        MDC.put(MDC_BRANCH_ID, branchId);
      }
      filterChain.doFilter(request, response);
    } finally {
      MDC.remove(MDC_TENANT_ID);
      MDC.remove(MDC_USER_ID);
      MDC.remove(MDC_BRANCH_ID);
    }
  }
}
