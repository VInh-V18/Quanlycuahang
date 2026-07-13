package com.quanlycuahang.erp.system.service;

import com.quanlycuahang.erp.auth.security.TenantContext;
import com.quanlycuahang.erp.common.exception.BusinessRuleException;
import com.quanlycuahang.erp.system.entity.Settings;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.repository.SettingsRepository;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Doc cau hinh he thong (lam tron, gia gom VAT, ban am kho, han muc no...) uu tien theo chi nhanh,
 * roi ve global (branch_id = NULL). Cache Redis vi doc rat nhieu (moi don POS deu can) — D4/Phan C.
 */
@Service
public class SettingsService {

  private static final String CACHE_PREFIX = "settings:";
  private static final Duration CACHE_TTL = Duration.ofMinutes(10);
  private static final String NULL_MARKER = " NULL ";

  public static final String KEY_ALLOW_NEGATIVE_STOCK = "allow_negative_stock";
  public static final String KEY_PRICE_INCLUDES_VAT_DEFAULT = "price_includes_vat_default";
  public static final String KEY_ROUNDING_UNIT = "rounding_unit";
  public static final String KEY_DEBT_LIMIT_DEFAULT = "debt_limit_default";
  public static final String KEY_SKU_PREFIX = "sku_prefix";
  public static final String KEY_ORDER_NUMBER_PREFIX = "order_number_prefix";
  public static final String KEY_INVOICE_NUMBER_PREFIX = "invoice_number_prefix";
  public static final String KEY_STORE_NAME = "store_name";
  public static final String KEY_STORE_SLOGAN = "store_slogan";
  public static final String KEY_STORE_TAX_CODE = "store_tax_code";
  public static final String KEY_STORE_ADDRESS = "store_address";
  public static final String KEY_STORE_PHONE = "store_phone";
  public static final String KEY_BANK_ACCOUNT_NAME = "bank_account_name";
  public static final String KEY_BANK_ACCOUNT_NUMBER = "bank_account_number";
  public static final String KEY_BANK_NAME = "bank_name";
  public static final String KEY_BANK_BIN = "bank_bin";
  public static final String KEY_BANK_QR_IMAGE_URL = "bank_qr_image_url";
  public static final String KEY_LOGIN_RATE_LIMIT_ATTEMPTS = "login_rate_limit_attempts";

  /**
   * Co tat nhanh rate-limit API chung (Prompt #3) khong can redeploy — "off" (tat han), "shadow"
   * (chi do + log, khong chan that, dung khi moi bat lai de xem co chan nham traffic hop le khong
   * truoc khi enforce that), "enforce" (chan that, mac dinh).
   */
  public static final String KEY_RATE_LIMIT_MODE = "rate_limit_mode";

  /**
   * Co tat nhanh job doi soat toan ven du lieu chay dem (Prompt #6) theo tung tenant - "true" (mac
   * dinh, chay binh thuong) / "false" (tat rieng cho tenant nay, vd tenant dang debug/import du
   * lieu lon co the co lech tam thoi). Khong anh huong duong chay THU CONG (POST
   * /admin/reconciliation/run) - chu cua hang tu bam thi luon chay duoc.
   */
  public static final String KEY_RECONCILIATION_JOB_ENABLED = "reconciliation_job_enabled";

  /**
   * Danh sach khoa duoc phep doc/sua qua trang Cai dat (FH-16) — chan cap nhat khoa la ngoai danh
   * sach nay de tranh API bi loi dung ghi de gia tri tuy y vao bang settings.
   */
  private static final List<String> EDITABLE_KEYS =
      List.of(
          KEY_PRICE_INCLUDES_VAT_DEFAULT,
          KEY_ROUNDING_UNIT,
          KEY_ALLOW_NEGATIVE_STOCK,
          KEY_DEBT_LIMIT_DEFAULT,
          KEY_ORDER_NUMBER_PREFIX,
          KEY_INVOICE_NUMBER_PREFIX,
          KEY_SKU_PREFIX,
          KEY_STORE_NAME,
          KEY_STORE_SLOGAN,
          KEY_STORE_TAX_CODE,
          KEY_STORE_ADDRESS,
          KEY_STORE_PHONE,
          KEY_BANK_ACCOUNT_NAME,
          KEY_BANK_ACCOUNT_NUMBER,
          KEY_BANK_NAME,
          KEY_BANK_BIN,
          KEY_BANK_QR_IMAGE_URL,
          KEY_LOGIN_RATE_LIMIT_ATTEMPTS,
          KEY_RATE_LIMIT_MODE,
          KEY_RECONCILIATION_JOB_ENABLED);

  private static final Set<String> BOOLEAN_KEYS =
      Set.of(
          KEY_PRICE_INCLUDES_VAT_DEFAULT, KEY_ALLOW_NEGATIVE_STOCK, KEY_RECONCILIATION_JOB_ENABLED);

  private static final Set<String> NUMERIC_KEYS =
      Set.of(KEY_ROUNDING_UNIT, KEY_DEBT_LIMIT_DEFAULT, KEY_LOGIN_RATE_LIMIT_ATTEMPTS);

  private static final Map<String, String> DEFAULTS =
      Map.ofEntries(
          Map.entry(KEY_PRICE_INCLUDES_VAT_DEFAULT, "true"),
          Map.entry(KEY_ROUNDING_UNIT, "1000"),
          Map.entry(KEY_ALLOW_NEGATIVE_STOCK, "false"),
          Map.entry(KEY_DEBT_LIMIT_DEFAULT, "5000000"),
          Map.entry(KEY_ORDER_NUMBER_PREFIX, "HD-"),
          Map.entry(KEY_INVOICE_NUMBER_PREFIX, "INV-"),
          Map.entry(KEY_SKU_PREFIX, "SP-"),
          Map.entry(KEY_STORE_NAME, ""),
          Map.entry(KEY_STORE_SLOGAN, ""),
          Map.entry(KEY_STORE_TAX_CODE, ""),
          Map.entry(KEY_STORE_ADDRESS, ""),
          Map.entry(KEY_STORE_PHONE, ""),
          Map.entry(KEY_BANK_ACCOUNT_NAME, ""),
          Map.entry(KEY_BANK_ACCOUNT_NUMBER, ""),
          Map.entry(KEY_BANK_NAME, ""),
          Map.entry(KEY_BANK_BIN, ""),
          Map.entry(KEY_BANK_QR_IMAGE_URL, ""),
          Map.entry(KEY_LOGIN_RATE_LIMIT_ATTEMPTS, "5"),
          Map.entry(KEY_RATE_LIMIT_MODE, "enforce"),
          Map.entry(KEY_RECONCILIATION_JOB_ENABLED, "true"));

  private static final Set<String> RATE_LIMIT_MODES = Set.of("off", "shadow", "enforce");

  private final SettingsRepository settingsRepository;
  private final BranchRepository branchRepository;
  private final StringRedisTemplate redisTemplate;

  public SettingsService(
      SettingsRepository settingsRepository,
      BranchRepository branchRepository,
      StringRedisTemplate redisTemplate) {
    this.settingsRepository = settingsRepository;
    this.branchRepository = branchRepository;
    this.redisTemplate = redisTemplate;
  }

  public String getValue(Long branchId, String key, String defaultValue) {
    String cacheKey = cacheKey(branchId, key);
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      return NULL_MARKER.equals(cached) ? defaultValue : cached;
    }

    String value = loadFromDb(branchId, key);
    redisTemplate.opsForValue().set(cacheKey, value == null ? NULL_MARKER : value, CACHE_TTL);
    return value == null ? defaultValue : value;
  }

  /**
   * Dung khi CHUA co TenantContext (vd doc gioi han dang nhap TRUOC khi xac thuc trong
   * AuthService.login) - getValue() thuong o tren dua vao TenantContext.get() ca cho cache key lan
   * cho @Filter Hibernate loc truy van; khi TenantContext dang null (luc chua dang nhap), ca 2 co
   * che nay deu suy bien thanh 1 khoa/1 tap ket qua DUNG CHUNG cho MOI tenant - 1 tenant tuy chinh
   * gia tri nay se anh huong toi tenant khac, hoac te hon la lam truy van nem loi (nhieu dong ket
   * qua) khi co tu 2 tenant tro len tuy chinh cung 1 khoa, gay loi dang nhap cho TOAN HE THONG
   * (phat hien khi rieng soat bao mat). Ham nay nhan tenantId TUONG MINH, khong dua vao
   * TenantContext, dam bao dung tenant va khong dung chung cache voi tenant khac.
   */
  public String getValueForTenant(Long tenantId, String key, String defaultValue) {
    String cacheKey = CACHE_PREFIX + tenantId + ":global:" + key;
    String cached = redisTemplate.opsForValue().get(cacheKey);
    if (cached != null) {
      return NULL_MARKER.equals(cached) ? defaultValue : cached;
    }
    String value =
        settingsRepository
            .findByTenantIdAndBranchIdIsNullAndKey(tenantId, key)
            .map(Settings::getValue)
            .orElse(null);
    redisTemplate.opsForValue().set(cacheKey, value == null ? NULL_MARKER : value, CACHE_TTL);
    return value == null ? defaultValue : value;
  }

  public boolean getBoolean(Long branchId, String key, boolean defaultValue) {
    return Boolean.parseBoolean(getValue(branchId, key, String.valueOf(defaultValue)));
  }

  public BigDecimal getBigDecimal(Long branchId, String key, BigDecimal defaultValue) {
    String raw = getValue(branchId, key, defaultValue == null ? null : defaultValue.toString());
    if (raw == null) {
      return null;
    }
    try {
      return new BigDecimal(raw);
    } catch (NumberFormatException ex) {
      // Phong thu lop 2: gia tri sai le da bi chan tu luc update() (xem validateValue()), nhung
      // van fallback an toan o day thay vi nem loi tho lam hong POS toan tenant neu co du lieu cu
      // sai sot lot qua (phat hien khi rieng soat).
      return defaultValue;
    }
  }

  /** Toan bo cau hinh global (branchId = null) hien thi tren trang Cai dat (FH-16). */
  public Map<String, String> getAllGlobal() {
    Map<String, String> result = new LinkedHashMap<>();
    for (String key : EDITABLE_KEYS) {
      result.put(key, getValue(null, key, DEFAULTS.get(key)));
    }
    return result;
  }

  @Transactional
  public Map<String, String> updateGlobal(Map<String, String> updates) {
    for (Map.Entry<String, String> entry : updates.entrySet()) {
      if (!EDITABLE_KEYS.contains(entry.getKey())) {
        throw new BusinessRuleException(
            "SETTINGS_INVALID_KEY", "Khóa cấu hình không hợp lệ: " + entry.getKey());
      }
      update(null, entry.getKey(), entry.getValue());
    }
    return getAllGlobal();
  }

  @Transactional
  public void update(Long branchId, String key, String value) {
    validateValue(key, value);
    Settings settings =
        (branchId != null
                ? settingsRepository.findByBranchIdAndKey(branchId, key)
                : settingsRepository.findByBranchIdIsNullAndKey(key))
            .orElseGet(Settings::new);
    if (settings.getId() == null) {
      settings.setKey(key);
      if (branchId != null) {
        settings.setBranch(branchRepository.getReferenceById(branchId));
      }
    }
    settings.setValue(value);
    settingsRepository.save(settings);
    redisTemplate.delete(cacheKey(branchId, key));
  }

  /**
   * Chan gia tri sai kieu NGAY LUC LUU thay vi de loi (vd NumberFormatException trong
   * getBigDecimal) roi tao lai o request khac - truoc day 1 gia tri rounding_unit sai (vd "1,000"
   * hoac rong) co the lam moi don POS ke tiep cua CA TENANT nem 500 cho toi khi admin sua lai (phat
   * hien khi rieng soat).
   */
  private void validateValue(String key, String value) {
    if (value == null) {
      return;
    }
    if (BOOLEAN_KEYS.contains(key)) {
      if (!"true".equalsIgnoreCase(value) && !"false".equalsIgnoreCase(value)) {
        throw new BusinessRuleException(
            "SETTINGS_INVALID_VALUE", "Giá trị cấu hình \"" + key + "\" phải là true hoặc false");
      }
      return;
    }
    if (NUMERIC_KEYS.contains(key)) {
      try {
        new BigDecimal(value);
      } catch (NumberFormatException ex) {
        throw new BusinessRuleException(
            "SETTINGS_INVALID_VALUE", "Giá trị cấu hình \"" + key + "\" phải là một số hợp lệ");
      }
    }
    if (KEY_RATE_LIMIT_MODE.equals(key) && !RATE_LIMIT_MODES.contains(value)) {
      throw new BusinessRuleException(
          "SETTINGS_INVALID_VALUE",
          "Giá trị cấu hình \"" + key + "\" phải là một trong: off, shadow, enforce");
    }
  }

  private String loadFromDb(Long branchId, String key) {
    if (branchId != null) {
      var branchSpecific = settingsRepository.findByBranchIdAndKey(branchId, key);
      if (branchSpecific.isPresent()) {
        return branchSpecific.get().getValue();
      }
    }
    return settingsRepository.findByBranchIdIsNullAndKey(key).map(Settings::getValue).orElse(null);
  }

  /**
   * Nhung tenantId hien tai vao cache key - branchId tuy la duy nhat toan he thong (khong tai su
   * dung giua tenant) nen it rui ro trung, nhung truong hop branchId = null (cau hinh global cua 1
   * tenant, vd ten cua hang) neu khong co tenantId se dung CHUNG 1 key cho MOI tenant, ro ri cau
   * hinh (ten/VAT/tai khoan ngan hang) giua cac cua hang khong lien quan.
   */
  private String cacheKey(Long branchId, String key) {
    Long tenantId = TenantContext.get();
    return CACHE_PREFIX + tenantId + ":" + (branchId == null ? "global" : branchId) + ":" + key;
  }
}
