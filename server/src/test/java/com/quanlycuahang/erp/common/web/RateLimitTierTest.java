package com.quanlycuahang.erp.common.web;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Prompt #3: unit test phan loai tang rate-limit theo method+path — dac biet ca GET /export phai
 * roi vao SENSITIVE thay vi bi "loi" vao READ chi vi la GET.
 */
class RateLimitTierTest {

  private MockHttpServletRequest request(String method, String uri) {
    return new MockHttpServletRequest(method, uri);
  }

  @Test
  void postToOrdersIsCheckoutTier() {
    assertThat(RateLimitTier.classifyAuthenticated(request("POST", "/api/v1/orders")))
        .isEqualTo(RateLimitTier.CHECKOUT);
  }

  @Test
  void postToOrderSubpathIsNotCheckoutTier() {
    // Vd POST /orders/{id}/cancel - KHONG phai checkout, chi POST / (tao don) moi la checkout.
    assertThat(RateLimitTier.classifyAuthenticated(request("POST", "/api/v1/orders/5/cancel")))
        .isEqualTo(RateLimitTier.DEFAULT);
  }

  @Test
  void plainGetIsReadTier() {
    assertThat(RateLimitTier.classifyAuthenticated(request("GET", "/api/v1/products")))
        .isEqualTo(RateLimitTier.READ);
  }

  @Test
  void getExportIsSensitiveTierDespiteBeingGet() {
    assertThat(RateLimitTier.classifyAuthenticated(request("GET", "/api/v1/orders/export")))
        .isEqualTo(RateLimitTier.SENSITIVE);
  }

  @Test
  void changePasswordIsSensitiveTier() {
    assertThat(RateLimitTier.classifyAuthenticated(request("POST", "/api/v1/auth/change-password")))
        .isEqualTo(RateLimitTier.SENSITIVE);
  }

  @Test
  void deleteIsSensitiveTier() {
    assertThat(RateLimitTier.classifyAuthenticated(request("DELETE", "/api/v1/customers/9")))
        .isEqualTo(RateLimitTier.SENSITIVE);
  }

  @Test
  void otherWritesAreDefaultTier() {
    assertThat(RateLimitTier.classifyAuthenticated(request("POST", "/api/v1/purchase-orders")))
        .isEqualTo(RateLimitTier.DEFAULT);
  }

  @Test
  void invoiceLookupGetIsPublicLookupEndpoint() {
    assertThat(
            RateLimitTier.isPublicLookupEndpoint(request("GET", "/api/v1/invoices/lookup/ABC123")))
        .isTrue();
  }

  @Test
  void otherPublicGetIsNotPublicLookupEndpoint() {
    assertThat(RateLimitTier.isPublicLookupEndpoint(request("GET", "/api/v1/settings/branding")))
        .isFalse();
  }
}
