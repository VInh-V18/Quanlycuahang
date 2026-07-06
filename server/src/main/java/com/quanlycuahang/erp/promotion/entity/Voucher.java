package com.quanlycuahang.erp.promotion.entity;

import com.quanlycuahang.erp.common.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

/**
 * used_count la denormalize co chu dich de verify nhanh o POS (D4, khong COUNT(*) moi
 * lan). @Version cho optimistic locking (giong Inventory) — thieu truoc day khien 2 don dung cung 1
 * voucher gan het luot dong thoi co the deu tang used_count thanh cong, vuot qua max_usage that su.
 */
@Entity
@Table(name = "vouchers")
@SQLDelete(sql = "UPDATE vouchers SET deleted_at = now() WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
public class Voucher extends BaseEntity {

  @Column(name = "code", nullable = false, unique = true)
  private String code;

  @Column(name = "discount_type", nullable = false)
  private String discountType;

  @Column(name = "discount_value", nullable = false)
  private BigDecimal discountValue;

  @Column(name = "min_order_amount", nullable = false)
  private BigDecimal minOrderAmount;

  @Column(name = "max_usage", nullable = false)
  private int maxUsage = 1;

  @Column(name = "used_count", nullable = false)
  private int usedCount;

  @Column(name = "expires_at")
  private OffsetDateTime expiresAt;

  @Column(name = "is_active", nullable = false)
  private boolean active = true;

  @Version
  @Column(name = "version", nullable = false)
  private Long version;

  public String getCode() {
    return code;
  }

  public void setCode(String code) {
    this.code = code;
  }

  public String getDiscountType() {
    return discountType;
  }

  public void setDiscountType(String discountType) {
    this.discountType = discountType;
  }

  public BigDecimal getDiscountValue() {
    return discountValue;
  }

  public void setDiscountValue(BigDecimal discountValue) {
    this.discountValue = discountValue;
  }

  public BigDecimal getMinOrderAmount() {
    return minOrderAmount;
  }

  public void setMinOrderAmount(BigDecimal minOrderAmount) {
    this.minOrderAmount = minOrderAmount;
  }

  public int getMaxUsage() {
    return maxUsage;
  }

  public void setMaxUsage(int maxUsage) {
    this.maxUsage = maxUsage;
  }

  public int getUsedCount() {
    return usedCount;
  }

  public void setUsedCount(int usedCount) {
    this.usedCount = usedCount;
  }

  public OffsetDateTime getExpiresAt() {
    return expiresAt;
  }

  public void setExpiresAt(OffsetDateTime expiresAt) {
    this.expiresAt = expiresAt;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }
}
