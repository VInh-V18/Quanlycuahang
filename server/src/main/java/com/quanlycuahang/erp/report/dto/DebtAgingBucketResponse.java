package com.quanlycuahang.erp.report.dto;

import java.math.BigDecimal;

/**
 * 1 muc tuoi no chuan (0-30/31-60/61-90/>90 ngay) — tinh tu ngay tao Debt (createdAt) den hien tai,
 * chi tinh cong no con du (amount > 0).
 */
public class DebtAgingBucketResponse {

  private String bucket;
  private BigDecimal totalAmount;
  private long debtCount;

  public DebtAgingBucketResponse() {}

  public DebtAgingBucketResponse(String bucket, BigDecimal totalAmount, long debtCount) {
    this.bucket = bucket;
    this.totalAmount = totalAmount;
    this.debtCount = debtCount;
  }

  public String getBucket() {
    return bucket;
  }

  public void setBucket(String bucket) {
    this.bucket = bucket;
  }

  public BigDecimal getTotalAmount() {
    return totalAmount;
  }

  public void setTotalAmount(BigDecimal totalAmount) {
    this.totalAmount = totalAmount;
  }

  public long getDebtCount() {
    return debtCount;
  }

  public void setDebtCount(long debtCount) {
    this.debtCount = debtCount;
  }
}
