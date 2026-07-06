package com.quanlycuahang.erp.operation.dto;

import java.math.BigDecimal;

/**
 * Tong hop 1 dong theo thue suat (VD: tat ca dong VAT 10% gop lai) — hoa don VN thuong tach rieng
 * tung muc thue suat thay vi chi hien 1 tong VAT duy nhat.
 */
public class VatBreakdownResponse {

  private BigDecimal vatRate;
  private BigDecimal taxableAmount;
  private BigDecimal vatAmount;

  public VatBreakdownResponse() {}

  public VatBreakdownResponse(BigDecimal vatRate, BigDecimal taxableAmount, BigDecimal vatAmount) {
    this.vatRate = vatRate;
    this.taxableAmount = taxableAmount;
    this.vatAmount = vatAmount;
  }

  public BigDecimal getVatRate() {
    return vatRate;
  }

  public void setVatRate(BigDecimal vatRate) {
    this.vatRate = vatRate;
  }

  public BigDecimal getTaxableAmount() {
    return taxableAmount;
  }

  public void setTaxableAmount(BigDecimal taxableAmount) {
    this.taxableAmount = taxableAmount;
  }

  public BigDecimal getVatAmount() {
    return vatAmount;
  }

  public void setVatAmount(BigDecimal vatAmount) {
    this.vatAmount = vatAmount;
  }
}
