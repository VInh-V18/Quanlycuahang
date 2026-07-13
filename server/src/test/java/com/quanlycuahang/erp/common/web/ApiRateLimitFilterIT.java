package com.quanlycuahang.erp.common.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.quanlycuahang.erp.AbstractIntegrationTest;
import com.quanlycuahang.erp.TestDataFactory;
import com.quanlycuahang.erp.system.service.SettingsService;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Prompt #3, buoc 4 (yeu cau bat buoc): integration test that voi Redis that (container tam thoi
 * cua Prompt #1) chung minh vuot nguong -> 429 kem Retry-After, duoi nguong -> di qua, va 2 che do
 * "shadow"/"off" khong chan that du vuot nguong (chi enforce moi chan that).
 */
class ApiRateLimitFilterIT extends AbstractIntegrationTest {

  @Autowired private ApiRateLimitFilter filter;
  @Autowired private SettingsService settingsService;
  @Autowired private TestDataFactory testDataFactory;

  private MockHttpServletRequest sensitiveDeleteRequest() {
    return new MockHttpServletRequest("DELETE", "/api/v1/customers/1");
  }

  @Test
  void underThresholdPassesThroughAndLeavesResponseUntouched() throws Exception {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantRlUnder");
    actingAsUser(tenant.owner());

    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);
    filter.doFilter(sensitiveDeleteRequest(), response, chain);

    verify(chain, times(1))
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    assertThat(response.getStatus()).isEqualTo(200);
  }

  @Test
  void exceedingSensitiveTierThresholdReturns429WithRetryAfter() throws Exception {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantRlOver");
    actingAsUser(tenant.owner());

    // SENSITIVE = 10 req/phut (RateLimitTier) - tieu het dung 10 luot truoc, luot thu 11 phai bi
    // chan. Moi lan dung request/response/chain MOI (giong 1 request HTTP that rieng biet).
    for (int i = 0; i < 10; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(sensitiveDeleteRequest(), response, mock(FilterChain.class));
      assertThat(response.getStatus()).isEqualTo(200);
    }

    MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
    FilterChain blockedChain = mock(FilterChain.class);
    filter.doFilter(sensitiveDeleteRequest(), blockedResponse, blockedChain);

    verify(blockedChain, never())
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    assertThat(blockedResponse.getStatus()).isEqualTo(429);
    assertThat(blockedResponse.getHeader(HttpHeaders.RETRY_AFTER)).isNotNull();
    assertThat(blockedResponse.getContentAsString()).contains("RATE_LIMITED");
  }

  @Test
  void shadowModeNeverBlocksEvenPastThreshold() throws Exception {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantRlShadow");
    actingAsUser(tenant.owner());
    settingsService.update(null, SettingsService.KEY_RATE_LIMIT_MODE, "shadow");

    for (int i = 0; i < 15; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);
      filter.doFilter(sensitiveDeleteRequest(), response, chain);
      // Shadow mode: du vuot nguong (>10), van PHAI di qua - chi do/log, khong chan that.
      verify(chain, times(1))
          .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  @Test
  void offModeNeverBlocksEvenPastThreshold() throws Exception {
    TestDataFactory.TestTenant tenant = testDataFactory.createTenantWithBranches("tenantRlOff");
    actingAsUser(tenant.owner());
    settingsService.update(null, SettingsService.KEY_RATE_LIMIT_MODE, "off");

    for (int i = 0; i < 15; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      FilterChain chain = mock(FilterChain.class);
      filter.doFilter(sensitiveDeleteRequest(), response, chain);
      verify(chain, times(1))
          .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
      assertThat(response.getStatus()).isEqualTo(200);
    }
  }

  private MockHttpServletRequest publicLookupRequest() {
    // Request MOI moi lan goi (khong tai su dung 1 object) - OncePerRequestFilter danh dau "da xu
    // ly" qua attribute tren CHINH request object, tai su dung se bi bo qua doFilterInternal hoan
    // toan o cac lan sau (luon "di qua" gia, khong con kiem tra rate-limit that).
    MockHttpServletRequest request =
        new MockHttpServletRequest("GET", "/api/v1/invoices/lookup/ABCDEF123456");
    request.setRemoteAddr("203.0.113.9");
    return request;
  }

  @Test
  void publicLookupEndpointIsRateLimitedByIpRegardlessOfAuthentication() throws Exception {
    // Khong actingAsUser() - endpoint cong khai, khong dang nhap, giong khach quet QR that.
    for (int i = 0; i < 30; i++) {
      MockHttpServletResponse response = new MockHttpServletResponse();
      filter.doFilter(publicLookupRequest(), response, mock(FilterChain.class));
      assertThat(response.getStatus()).isEqualTo(200);
    }

    MockHttpServletResponse blockedResponse = new MockHttpServletResponse();
    FilterChain blockedChain = mock(FilterChain.class);
    filter.doFilter(publicLookupRequest(), blockedResponse, blockedChain);
    verify(blockedChain, never())
        .doFilter(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.any());
    assertThat(blockedResponse.getStatus()).isEqualTo(429);
  }
}
