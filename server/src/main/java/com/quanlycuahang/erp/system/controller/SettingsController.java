package com.quanlycuahang.erp.system.controller;

import com.quanlycuahang.erp.auth.security.JwtService;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.system.dto.BrandingResponse;
import com.quanlycuahang.erp.system.dto.SettingsOverviewResponse;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Cai dat he thong (FH-16) — doc/sua cau hinh global dang key-value (bang settings, Phase 3). */
@RestController
@RequestMapping("/api/v1/settings")
public class SettingsController {

  private final SettingsService settingsService;
  private final JwtService jwtService;

  public SettingsController(SettingsService settingsService, JwtService jwtService) {
    this.settingsService = settingsService;
    this.jwtService = jwtService;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('settings:view')")
  public ResponseEntity<ApiResponse<SettingsOverviewResponse>> get() {
    Map<String, String> systemInfo =
        Map.of(
            "accessTokenTtlMinutes", String.valueOf(jwtService.getAccessTokenTtlMinutes()),
            "refreshTokenTtlDays", String.valueOf(jwtService.getRefreshTokenTtlDays()));
    return ResponseEntity.ok(
        ApiResponse.success(
            new SettingsOverviewResponse(settingsService.getAllGlobal(), systemInfo)));
  }

  @PutMapping
  @PreAuthorize("hasAuthority('settings:update')")
  public ResponseEntity<ApiResponse<Map<String, String>>> update(
      @RequestBody Map<String, String> updates) {
    return ResponseEntity.ok(ApiResponse.success(settingsService.updateGlobal(updates)));
  }

  /**
   * Ten/khau hieu cua hang hien thi CONG KHAI (Sidebar, trang dang nhap luc chua dang nhap) — khong
   * doi hoi quyen gi, permitAll trong SecurityConfig. Chi tra dung 2 truong nay, khong ghep them
   * cau hinh nhay cam khac cua Settings.
   *
   * <p>Multi-tenant: luc chua dang nhap (trang Dang nhap) khong co JWT nen khong co TenantContext —
   * he thong dung 1 trang dang nhap chung cho moi tenant (khong chon "cua hang nao" truoc), nen
   * khong the biet phai tra ten cua tenant nao. Neu van goi settingsService luc nay, Hibernate se
   * bo qua @Filter tenant (chi bat khi co TenantContext) va co the tra ve ten cua MOT TENANT BAT KY
   * — ro ri nham cua hang. Vi vay chua dang nhap thi luon tra rong (Frontend tu hien ten mac dinh
   * chung); da dang nhap (goi lai sau khi vao he thong, co JWT) thi tra dung ten tenant cua nguoi
   * dang nhap nhu binh thuong.
   */
  @GetMapping("/branding")
  public ResponseEntity<ApiResponse<BrandingResponse>> getBranding() {
    if (TenantContext.get() == null) {
      return ResponseEntity.ok(ApiResponse.success(new BrandingResponse("", "")));
    }
    return ResponseEntity.ok(
        ApiResponse.success(
            new BrandingResponse(
                settingsService.getValue(null, SettingsService.KEY_STORE_NAME, ""),
                settingsService.getValue(null, SettingsService.KEY_STORE_SLOGAN, ""))));
  }
}
