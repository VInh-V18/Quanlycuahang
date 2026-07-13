package com.quanlycuahang.erp.reconciliation.controller;

import com.quanlycuahang.erp.auth.security.CurrentUserProvider;
import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.dto.ApiResponse;
import com.quanlycuahang.erp.common.metrics.BusinessMetrics;
import com.quanlycuahang.erp.reconciliation.dto.ReconciliationRunResponse;
import com.quanlycuahang.erp.reconciliation.entity.ReconciliationRun;
import com.quanlycuahang.erp.reconciliation.repository.ReconciliationFindingRepository;
import com.quanlycuahang.erp.reconciliation.service.ReconciliationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Doi soat toan ven du lieu (Prompt #6, P1) — chi owner/manager (quyen reconciliation:run).
 *
 * <p>Prompt #10 (P3, don dep no ky thuat): bo 2 endpoint history()/detail() (GET khong tham so va
 * GET /{runId}) — khong FE nao goi toi (man hinh xem lich su doi soat chua tung duoc lam, chi co
 * canh bao tren Dashboard qua open-count), da xac nhan voi nguoi dung truoc khi xoa. Muon xem lai
 * lich su 1 tenant cu the, tra truc tiep bang reconciliation_runs/reconciliation_findings qua psql
 * cho den khi man hinh nay duoc lam that.
 */
@RestController
@RequestMapping("/api/v1/admin/reconciliation")
public class ReconciliationController {

  private final ReconciliationService reconciliationService;
  private final ReconciliationFindingRepository findingRepository;
  private final CurrentUserProvider currentUserProvider;
  private final BusinessMetrics businessMetrics;

  public ReconciliationController(
      ReconciliationService reconciliationService,
      ReconciliationFindingRepository findingRepository,
      CurrentUserProvider currentUserProvider,
      BusinessMetrics businessMetrics) {
    this.reconciliationService = reconciliationService;
    this.findingRepository = findingRepository;
    this.currentUserProvider = currentUserProvider;
    this.businessMetrics = businessMetrics;
  }

  @PostMapping("/run")
  @PreAuthorize("hasAuthority('reconciliation:run')")
  public ResponseEntity<ApiResponse<ReconciliationRunResponse>> run() {
    Long userId = currentUserProvider.getCurrentUser().map(u -> u.getId()).orElse(null);
    ReconciliationRunResponse response =
        reconciliationService.runForTenant(
            TenantContext.get(), ReconciliationRun.TRIGGER_MANUAL, userId);
    return ResponseEntity.ok(ApiResponse.success(response));
  }

  /**
   * So finding OPEN cua tenant hien tai - dung cho canh bao tren DashboardPage (chi role
   * owner/manager, guard o cung 1 quyen reconciliation:run vi day cung la thong tin quan tri).
   */
  @GetMapping("/open-count")
  @PreAuthorize("hasAuthority('reconciliation:run')")
  public ResponseEntity<ApiResponse<Long>> openFindingsCount() {
    long count = findingRepository.countByStatus("open");
    // Cap nhat gauge Prometheus moi lan Dashboard hoi (Prompt #8, P2 quan sat) - gauge chi doc gia
    // tri hien tai luc scrape, khong tu lam moi, nen can 1 noi ghi de gia tri thuong xuyen.
    businessMetrics.setReconciliationFindingsOpen(TenantContext.get(), count);
    return ResponseEntity.ok(ApiResponse.success(count));
  }
}
