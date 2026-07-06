package com.quanlycuahang.erp.inventory.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Tinh gia von binh quan gia quyen di dong (B4) — Java thuan, khong phu thuoc Spring context, de
 * unit test khong can khoi dong ApplicationContext (Phase 11). Gia von chi thay doi khi NHAP hang,
 * khong doi khi ban.
 *
 * <p>giá_vốn_mới = (tồn_hiện_tại × giá_vốn_cũ + số_lượng_nhập × giá_nhập) / (tồn_hiện_tại +
 * số_lượng_nhập)
 */
public final class AverageCostService {

  private AverageCostService() {}

  public static BigDecimal calculateNewCost(
      BigDecimal currentStock,
      BigDecimal currentCost,
      BigDecimal incomingQty,
      BigDecimal incomingPrice) {
    BigDecimal totalStock = currentStock.add(incomingQty);
    if (totalStock.compareTo(BigDecimal.ZERO) <= 0) {
      // Ton + nhap <= 0 (hiem, vd nhap bu am) -> lay thang gia nhap moi nhat lam gia von.
      return incomingPrice.setScale(0, RoundingMode.HALF_UP);
    }
    BigDecimal numerator =
        currentStock.multiply(currentCost).add(incomingQty.multiply(incomingPrice));
    return numerator.divide(totalStock, 0, RoundingMode.HALF_UP);
  }
}
