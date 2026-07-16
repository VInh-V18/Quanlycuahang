package com.quanlycuahang.erp.sales.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class EditOrderRequest {

  @NotEmpty @Valid private List<EditOrderLineRequest> lines;

  public List<EditOrderLineRequest> getLines() {
    return lines;
  }

  public void setLines(List<EditOrderLineRequest> lines) {
    this.lines = lines;
  }
}
