package com.quanlycuahang.erp.reconciliation.service;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.auth.security.TenantSessionBinder;
import com.quanlycuahang.erp.reconciliation.dto.ReconciliationRunResponse;
import com.quanlycuahang.erp.reconciliation.entity.ReconciliationRun;
import com.quanlycuahang.erp.system.entity.Tenant;
import com.quanlycuahang.erp.system.repository.TenantRepository;
import com.quanlycuahang.erp.system.service.SettingsService;
import jakarta.persistence.EntityManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Chay doi soat toan ven du lieu dinh ky moi dem cho TUNG tenant dang hoat dong (Prompt #6) — moi
 * tenant chay rieng, loi 1 tenant KHONG duoc lam hong cac tenant con lai (try/catch quanh tung vong
 * lap, khong de 1 exception thoat ra ngoai lam dung ca job).
 *
 * <p>Tu bind TenantContext + Hibernate Session rieng cho tung tenant qua {@link
 * TenantSessionBinder} (giong dung mau InvoiceEmailService.sendInvoiceEmailAsync — ThreadLocal
 * khong tu ke thua sang thread cua scheduler, khac han thread xu ly 1 request HTTP thuong).
 */
@Component
public class ReconciliationScheduledJob {

  private static final Logger log = LoggerFactory.getLogger(ReconciliationScheduledJob.class);

  private final TenantRepository tenantRepository;
  private final TenantSessionBinder tenantSessionBinder;
  private final SettingsService settingsService;
  private final ReconciliationService reconciliationService;

  public ReconciliationScheduledJob(
      TenantRepository tenantRepository,
      TenantSessionBinder tenantSessionBinder,
      SettingsService settingsService,
      ReconciliationService reconciliationService) {
    this.tenantRepository = tenantRepository;
    this.tenantSessionBinder = tenantSessionBinder;
    this.settingsService = settingsService;
    this.reconciliationService = reconciliationService;
  }

  /**
   * 2 gio sang moi ngay (gio Viet Nam) - luc it tai nhat, sau khi da qua nua dem nhung truoc gio mo
   * cua hang thuong ngay.
   */
  @Scheduled(cron = "0 0 2 * * *", zone = "Asia/Ho_Chi_Minh")
  public void runNightlyForAllTenants() {
    for (Tenant tenant : tenantRepository.findByActiveTrueOrderByIdAsc()) {
      runForOneTenant(tenant.getId());
    }
  }

  private void runForOneTenant(Long tenantId) {
    TenantContext.set(tenantId);
    EntityManager entityManager = tenantSessionBinder.bind(tenantId);
    try {
      boolean enabled =
          settingsService.getBoolean(null, SettingsService.KEY_RECONCILIATION_JOB_ENABLED, true);
      if (!enabled) {
        log.info("Bo qua doi soat dem cho tenant {} - da tat qua cai dat rieng tenant", tenantId);
        return;
      }
      ReconciliationRunResponse result =
          reconciliationService.runForTenant(tenantId, ReconciliationRun.TRIGGER_SCHEDULED, null);
      if (result.getFindingsCount() > 0) {
        log.warn(
            "Doi soat dem tenant {} phat hien {} cho lech (run id={})",
            tenantId,
            result.getFindingsCount(),
            result.getId());
      }
    } catch (RuntimeException ex) {
      // Khong de 1 tenant loi lam dung ca vong lap - cac tenant con lai van phai duoc doi soat.
      log.error("Doi soat dem that bai cho tenant {}", tenantId, ex);
    } finally {
      tenantSessionBinder.unbind(entityManager);
      TenantContext.clear();
    }
  }
}
