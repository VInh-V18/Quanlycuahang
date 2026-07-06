package com.quanlycuahang.erp.partner.dto;

import java.math.BigDecimal;

public class DebtPartnerAgingResponse {

  private Long partnerId;
  private String partnerName;
  private BigDecimal totalDebt;
  private BigDecimal bucket0to7;
  private BigDecimal bucket8to30;
  private BigDecimal bucketOver30;

  public Long getPartnerId() {
    return partnerId;
  }

  public void setPartnerId(Long partnerId) {
    this.partnerId = partnerId;
  }

  public String getPartnerName() {
    return partnerName;
  }

  public void setPartnerName(String partnerName) {
    this.partnerName = partnerName;
  }

  public BigDecimal getTotalDebt() {
    return totalDebt;
  }

  public void setTotalDebt(BigDecimal totalDebt) {
    this.totalDebt = totalDebt;
  }

  public BigDecimal getBucket0to7() {
    return bucket0to7;
  }

  public void setBucket0to7(BigDecimal bucket0to7) {
    this.bucket0to7 = bucket0to7;
  }

  public BigDecimal getBucket8to30() {
    return bucket8to30;
  }

  public void setBucket8to30(BigDecimal bucket8to30) {
    this.bucket8to30 = bucket8to30;
  }

  public BigDecimal getBucketOver30() {
    return bucketOver30;
  }

  public void setBucketOver30(BigDecimal bucketOver30) {
    this.bucketOver30 = bucketOver30;
  }
}
