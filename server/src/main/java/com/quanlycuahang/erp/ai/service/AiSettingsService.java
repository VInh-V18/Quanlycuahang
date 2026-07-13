package com.quanlycuahang.erp.ai.service;

import com.quanlycuahang.erp.ai.security.AiKeyEncryptionService;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.system.service.SettingsService;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Cau hinh AI Assistant theo TUNG TENANT (Prompt #11) - dung lai ha tang {@link SettingsService}
 * (bang settings, cache Redis) nhung KHONG dua 2 khoa nay vao {@code EDITABLE_KEYS} cua
 * SettingsService - trang Cai dat chung (FH-16, {@code getAllGlobal()}/{@code updateGlobal()}) chi
 * tra ve dung danh sach EDITABLE_KEYS, nen khoa API (da ma hoa) se KHONG BAO GIO bi tra ve cho FE
 * qua duong do, du la ban da ma hoa. Endpoint rieng cho AI (xem AiAssistantController) la noi DUY
 * NHAT doc/ghi 2 khoa nay, va khi doc chi tra ve "da cau hinh hay chua" (boolean), KHONG BAO GIO
 * tra ve gia tri khoa API that (da giai ma hay chua) ve phia FE.
 */
@Service
public class AiSettingsService {

  static final String KEY_API_KEY_ENCRYPTED = "ai.provider.api_key_encrypted";
  static final String KEY_ENABLED = "ai.provider.enabled";

  private final SettingsService settingsService;
  private final AiKeyEncryptionService encryptionService;

  public AiSettingsService(
      SettingsService settingsService, AiKeyEncryptionService encryptionService) {
    this.settingsService = settingsService;
    this.encryptionService = encryptionService;
  }

  /**
   * Khoa API da giai ma cua tenant hien tai (TenantContext) - CHI dung noi bo de goi Claude API,
   * KHONG BAO GIO tra ve FE. Rong neu tenant chua bat AI hoac chua cau hinh khoa.
   */
  public Optional<String> getApiKey() {
    boolean enabled = "true".equals(settingsService.getValue(null, KEY_ENABLED, "false"));
    if (!enabled) {
      return Optional.empty();
    }
    String encrypted = settingsService.getValue(null, KEY_API_KEY_ENCRYPTED, null);
    if (encrypted == null || encrypted.isBlank()) {
      return Optional.empty();
    }
    return Optional.of(encryptionService.decrypt(encrypted));
  }

  /**
   * True neu tenant hien tai da bat + cau hinh khoa API (dung cho FE hien trang thai, khong lo gia
   * tri khoa that).
   */
  public boolean isConfigured() {
    return getApiKey().isPresent();
  }

  @Transactional
  public void updateApiKey(String plainApiKey) {
    if (plainApiKey == null || plainApiKey.isBlank()) {
      throw new BusinessRuleException("AI_API_KEY_REQUIRED", "Khoá API không được để trống");
    }
    if (!encryptionService.isConfigured()) {
      throw new BusinessRuleException(
          "AI_ENCRYPTION_NOT_CONFIGURED",
          "Máy chủ chưa cấu hình khoá mã hoá (AI_SETTINGS_ENCRYPTION_KEY) — liên hệ quản trị hệ"
              + " thống trước khi lưu khoá API");
    }
    settingsService.update(null, KEY_API_KEY_ENCRYPTED, encryptionService.encrypt(plainApiKey));
    settingsService.update(null, KEY_ENABLED, "true");
  }

  @Transactional
  public void disable() {
    settingsService.update(null, KEY_ENABLED, "false");
  }
}
