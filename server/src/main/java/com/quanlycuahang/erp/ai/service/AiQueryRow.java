package com.quanlycuahang.erp.ai.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Cac dong ket qua tho doc tu 4 view whitelist (V30) - dung lam input cho AI tool-calling. */
public final class AiQueryRow {

  private AiQueryRow() {}

  public record RevenueRow(LocalDate day, Long branchId, BigDecimal revenue, long orderCount) {}

  public record TopProductRow(
      Long productId,
      String productName,
      String sku,
      Long branchId,
      BigDecimal quantitySold,
      BigDecimal revenue) {}

  public record InventoryRow(
      Long productId,
      String productName,
      String sku,
      Long branchId,
      BigDecimal stock,
      BigDecimal minStock) {}

  public record DebtAgingRow(
      String direction, String agingBucket, BigDecimal totalAmount, long debtCount) {}

  /**
   * 1 dong doanh so ban theo TUNG NGAY cua 1 san pham (Prompt #12, phuc vu du bao nhap hang qua
   * Python ML service - can chi tiet theo ngay de phat hien ngay ban bat thuong, khac {@link
   * TopProductRow} chi tong hop 1 con so duy nhat cho ca 30 ngay).
   */
  public record DailySalesRow(
      Long productId, Long branchId, LocalDate saleDay, BigDecimal quantitySold) {}
}
