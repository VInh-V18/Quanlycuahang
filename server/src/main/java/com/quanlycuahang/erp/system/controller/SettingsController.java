package com.quanlycuahang.erp.system.controller;

import com.quanlycuahang.erp.auth.security.JwtService;
import com.quanlycuahang.erp.common.dto.ApiResponse;
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
}
