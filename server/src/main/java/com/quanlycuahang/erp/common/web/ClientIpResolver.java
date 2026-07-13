package com.quanlycuahang.erp.common.web;

import jakarta.servlet.http.HttpServletRequest;

/**
 * Xac dinh IP that cua client qua header X-Forwarded-For, dung cho khoa rate-limit dang nhap
 * (AuthController/PlatformAdminAuthController).
 *
 * <p>nginx (docker/nginx.conf) dung $proxy_add_x_forwarded_for de sinh header nay - co che nay NOI
 * THEM IP nginx quan sat duoc vao CUOI danh sach hien co (khong thay the), nen neu client tu gui
 * san 1 gia tri X-Forwarded-For gia, gia tri do van dung o vi tri DAU danh sach. Lay phan tu DAU
 * tien (nhu truoc day) chinh la lay dung gia tri client tu bia - cho phep vo hieu hoa hoan toan
 * gioi han chong do mat khau chi bang cach doi header nay moi lan goi (phat hien khi rieng soat bao
 * mat). Phan tu CUOI danh sach luon la IP do chinh nginx (hop tin cay gan nhat, khong the client
 * gia mao) ghi vao, nen phai lay phan tu CUOI.
 */
public final class ClientIpResolver {

  private ClientIpResolver() {}

  public static String resolve(HttpServletRequest request) {
    String forwardedFor = request.getHeader("X-Forwarded-For");
    if (forwardedFor != null && !forwardedFor.isBlank()) {
      String[] parts = forwardedFor.split(",");
      return parts[parts.length - 1].trim();
    }
    return request.getRemoteAddr();
  }
}
