package com.quanlycuahang.erp.dashboard.controller;

import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.dashboard.dto.DashboardSummaryResponse;
import com.quanlycuahang.erp.dashboard.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Trang Tong quan hien thi cho moi vai tro dang nhap — khong gioi han theo 1 quyen cu the. */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

  private final DashboardService dashboardService;

  public DashboardController(DashboardService dashboardService) {
    this.dashboardService = dashboardService;
  }

  @GetMapping("/summary")
  public ResponseEntity<ApiResponse<DashboardSummaryResponse>> summary(
      @RequestParam(required = false) Long branchId) {
    return ResponseEntity.ok(ApiResponse.success(dashboardService.summary(branchId)));
  }
}
