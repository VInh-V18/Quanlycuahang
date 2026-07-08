package com.quanlycuahang.erp.sales.statemachine;

/**
 * State machine don hang (B4): draft -> completed -> partially_returned -> fully_returned; draft ->
 * cancelled; completed -> cancelled (chi trong ngay, can quyen Quan ly). Validate tap trung tai
 * canTransition — khong rai if-else khap Service.
 */
public enum OrderStatus {
  DRAFT("draft"),
  COMPLETED("completed"),
  PARTIALLY_RETURNED("partially_returned"),
  FULLY_RETURNED("fully_returned"),
  CANCELLED("cancelled");

  private final String value;

  OrderStatus(String value) {
    this.value = value;
  }

  public String getValue() {
    return value;
  }

  public static OrderStatus fromValue(String value) {
    for (OrderStatus status : values()) {
      if (status.value.equals(value)) {
        return status;
      }
    }
    throw new IllegalArgumentException("Trang thai don khong hop le: " + value);
  }

  public static boolean canTransition(OrderStatus from, OrderStatus to) {
    return switch (from) {
      case DRAFT -> to == COMPLETED || to == CANCELLED;
      case COMPLETED -> to == PARTIALLY_RETURNED || to == FULLY_RETURNED || to == CANCELLED;
      case PARTIALLY_RETURNED -> to == FULLY_RETURNED;
      case FULLY_RETURNED, CANCELLED -> false;
    };
  }
}
