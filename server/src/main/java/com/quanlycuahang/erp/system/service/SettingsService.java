package com.quanlycuahang.erp.system.service;

import com.quanlycuahang.erp.system.entity.Settings;
import com.quanlycuahang.erp.system.repository.BranchRepository;
import com.quanlycuahang.erp.system.repository.SettingsRepository;
import java.math.BigDecimal;
import java.time.Duration;
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
  public static final String KEY_STORE_TAX_CODE = "store_tax_code";

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

  public boolean getBoolean(Long branchId, String key, boolean defaultValue) {
    return Boolean.parseBoolean(getValue(branchId, key, String.valueOf(defaultValue)));
  }

  public BigDecimal getBigDecimal(Long branchId, String key, BigDecimal defaultValue) {
    String raw = getValue(branchId, key, defaultValue == null ? null : defaultValue.toString());
    return raw == null ? null : new BigDecimal(raw);
  }

  @Transactional
  public void update(Long branchId, String key, String value) {
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

  private String loadFromDb(Long branchId, String key) {
    if (branchId != null) {
      var branchSpecific = settingsRepository.findByBranchIdAndKey(branchId, key);
      if (branchSpecific.isPresent()) {
        return branchSpecific.get().getValue();
      }
    }
    return settingsRepository.findByBranchIdIsNullAndKey(key).map(Settings::getValue).orElse(null);
  }

  private String cacheKey(Long branchId, String key) {
    return CACHE_PREFIX + (branchId == null ? "global" : branchId) + ":" + key;
  }
}
