package com.quanlycuahang.erp.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.quanlycuahang.erp.auth.security.TenantContext;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Prompt #8 (P2, quan sat): xac nhan RequestContextMdcFilter gan dung tenantId/userId/branchId vao
 * MDC TRONG LUC filter chain chay (moi dong log nghiep vu trong request se tu co 3 truong nay), va
 * XOA het sau khi request xong (khong de thread pool tai su dung lam request sau ke thua nham).
 */
class RequestContextMdcFilterTest {

  @AfterEach
  void tearDown() {
    TenantContext.clear();
    SecurityContextHolder.clearContext();
    MDC.clear();
  }

  @Test
  void putsTenantUserAndBranchIntoMdcDuringChainThenClearsAfter() throws Exception {
    TenantContext.set(42L);
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("owner-test", null, List.of()));

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/inventory");
    request.setParameter("branchId", "7");
    MockHttpServletResponse response = new MockHttpServletResponse();

    String[] capturedTenantId = new String[1];
    String[] capturedUserId = new String[1];
    String[] capturedBranchId = new String[1];
    FilterChain chain =
        (req, res) -> {
          capturedTenantId[0] = MDC.get("tenantId");
          capturedUserId[0] = MDC.get("userId");
          capturedBranchId[0] = MDC.get("branchId");
        };

    new RequestContextMdcFilter().doFilter(request, response, chain);

    assertThat(capturedTenantId[0]).isEqualTo("42");
    assertThat(capturedUserId[0]).isEqualTo("owner-test");
    assertThat(capturedBranchId[0]).isEqualTo("7");

    // Sau khi doFilter tra ve, MDC PHAI duoc don sach (finally trong filter).
    assertThat(MDC.get("tenantId")).isNull();
    assertThat(MDC.get("userId")).isNull();
    assertThat(MDC.get("branchId")).isNull();
  }

  @Test
  void skipsBranchIdWhenNotPresentAsQueryParam() throws Exception {
    TenantContext.set(1L);
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/categories");
    MockHttpServletResponse response = new MockHttpServletResponse();

    String[] capturedBranchId = new String[1];
    FilterChain chain = (req, res) -> capturedBranchId[0] = MDC.get("branchId");

    new RequestContextMdcFilter().doFilter(request, response, chain);

    assertThat(capturedBranchId[0]).isNull();
  }
}
