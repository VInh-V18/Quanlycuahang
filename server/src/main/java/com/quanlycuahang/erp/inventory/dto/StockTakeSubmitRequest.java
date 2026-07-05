package com.quanlycuahang.erp.inventory.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public class StockTakeSubmitRequest {

  @NotEmpty @Valid private List<StockTakeItemCountRequest> items;

  public List<StockTakeItemCountRequest> getItems() {
    return items;
  }

  public void setItems(List<StockTakeItemCountRequest> items) {
    this.items = items;
  }
}
