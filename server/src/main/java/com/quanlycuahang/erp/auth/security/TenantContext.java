package com.quanlycuahang.erp.auth.security;

/**
 * Luu tenantId cua request hien tai (ThreadLocal) — JwtAuthenticationFilter ghi vao ngay dau
 * request (sau khi xac thuc token thanh cong), TenantFilter doc gia tri nay de bat
 * Hibernate @Filter. PHAI xoa (clear) o cuoi moi request (finally) — thread duoc tai su dung qua
 * thread pool, khong xoa se lam request SAU (co the cua tenant khac, hoac request cong khai khong
 * dang nhap) vo tinh ke thua tenantId cua request TRUOC.
 */
public final class TenantContext {

  private static final ThreadLocal<Long> CURRENT_TENANT_ID = new ThreadLocal<>();

  private TenantContext() {}

  public static void set(Long tenantId) {
    CURRENT_TENANT_ID.set(tenantId);
  }

  public static Long get() {
    return CURRENT_TENANT_ID.get();
  }

  public static void clear() {
    CURRENT_TENANT_ID.remove();
  }
}
