package com.quanlycuahang.erp.partner.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class DebtHistoryEventResponse {

  private Instant eventAt;
  private String label;
  private String referenceCode;
  private BigDecimal amount;

  public Instant getEventAt() {
    return eventAt;
  }

  public void setEventAt(Instant eventAt) {
    this.eventAt = eventAt;
  }

  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public String getReferenceCode() {
    return referenceCode;
  }

  public void setReferenceCode(String referenceCode) {
    this.referenceCode = referenceCode;
  }

  public BigDecimal getAmount() {
    return amount;
  }

  public void setAmount(BigDecimal amount) {
    this.amount = amount;
  }
}
