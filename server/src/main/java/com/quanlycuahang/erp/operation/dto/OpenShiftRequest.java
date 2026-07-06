package com.quanlycuahang.erp.operation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class OpenShiftRequest {

  @NotNull
  @PositiveOrZero
  private BigDecimal openingCash;

  private String note;

  public BigDecimal getOpeningCash() {
    return openingCash;
  }

  public void setOpeningCash(BigDecimal openingCash) {
    this.openingCash = openingCash;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
