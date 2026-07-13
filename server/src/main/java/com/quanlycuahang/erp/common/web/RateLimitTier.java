package com.quanlycuahang.erp.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;

/**
 * Phan tang gioi han API chung (Prompt #3) theo nhom endpoint — thay vi 1 muc phang 100 req/phut
 * cho MOI API nhu truoc (qua chat cho POS checkout luc dong thoi nhieu thu ngan, qua long cho
 * endpoint nhay cam nhu doi mat khau/xoa du lieu/xuat Excel).
 */
enum RateLimitTier {
  /** POST /api/v1/orders (checkout POS) — nghiep vu chinh, can nguong cao. */
  CHECKOUT(120, Duration.ofMinutes(1)),
  /** Moi GET da dang nhap — doc du lieu, rong rai. */
  READ(600, Duration.ofMinutes(1)),
  /** Doi mat khau, xoa du lieu (DELETE), xuat Excel — chat vi anh huong du lieu/tai nguyen nang. */
  SENSITIVE(10, Duration.ofMinutes(1)),
  /** Moi POST/PUT/PATCH da dang nhap khac (khong phai checkout/nhay cam). */
  DEFAULT(100, Duration.ofMinutes(1)),
  /** Endpoint cong khai tra cuu hoa don (khong dang nhap) — theo IP, chong do quet ma. */
  PUBLIC_LOOKUP(30, Duration.ofMinutes(1));

  private final long capacity;
  private final Duration period;

  RateLimitTier(long capacity, Duration period) {
    this.capacity = capacity;
    this.period = period;
  }

  long capacity() {
    return capacity;
  }

  Duration period() {
    return period;
  }

  /**
   * Phan loai 1 request DA XAC THUC vao dung nhom — kiem tra SENSITIVE truoc GET/READ vi cac
   * endpoint xuat Excel (vd GET /orders/export, GET /inventory/export) dung phuong thuc GET nhung
   * van phai chiu muc chat hon (khong duoc "loi" vao nhom READ rong rai chi vi la GET).
   */
  static RateLimitTier classifyAuthenticated(HttpServletRequest request) {
    String method = request.getMethod();
    String path = request.getRequestURI();
    if ("POST".equals(method) && "/api/v1/orders".equals(path)) {
      return CHECKOUT;
    }
    if ("DELETE".equals(method) || path.endsWith("/change-password") || path.endsWith("/export")) {
      return SENSITIVE;
    }
    if ("GET".equals(method)) {
      return READ;
    }
    return DEFAULT;
  }

  static boolean isPublicLookupEndpoint(HttpServletRequest request) {
    return "GET".equals(request.getMethod())
        && request.getRequestURI().startsWith("/api/v1/invoices/lookup/");
  }
}
