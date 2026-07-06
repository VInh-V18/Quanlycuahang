package com.quanlycuahang.erp.operation.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

public class CloseShiftRequest {

  @NotNull
  @PositiveOrZero
  private BigDecimal actualCash;

  private String note;

  public BigDecimal getActualCash() {
    return actualCash;
  }

  public void setActualCash(BigDecimal actualCash) {
    this.actualCash = actualCash;
  }

  public String getNote() {
    return note;
  }

  public void setNote(String note) {
    this.note = note;
  }
}
