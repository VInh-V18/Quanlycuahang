package com.quanlycuahang.erp.inventory.service;

import com.quanlycuahang.erp.inventory.entity.InventoryTransaction;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

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

  /**
   * Tinh lai gia von HIEN TAI bang cach phat lai (replay) toan bo lich su giao dich kho cua 1 san
   * pham/chi nhanh theo dung thu tu thoi gian — dung khi sua gia 1 phieu nhap cu (nhap sai gia luc
   * tao phieu). Gia von chi doi o giao dich type=purchase (dung cong thuc B4 o tren), cac loai khac
   * (sale, stock_take, customer_return, cancel...) chi lam thay doi ton (transactionsInOrder da
   * mang dau +/- san, xem noi tao tung loai giao dich).
   *
   * <p>Tra kem ton kho tinh duoc sau khi phat lai — Service goi ham nay PHAI doi chieu voi
   * Inventory.stock hien tai; neu lech nghia la lich su khong day du/nhat quan, KHONG duoc dung ket
   * qua gia von tra ve.
   */
  public static ReplayResult replay(
      List<InventoryTransaction> transactionsInOrder,
      Long correctedTransactionId,
      BigDecimal correctedUnitPrice) {
    BigDecimal stock = BigDecimal.ZERO;
    BigDecimal cost = BigDecimal.ZERO;
    for (InventoryTransaction tx : transactionsInOrder) {
      if ("purchase".equals(tx.getType())) {
        BigDecimal price =
            tx.getId().equals(correctedTransactionId) ? correctedUnitPrice : tx.getUnitCost();
        cost = calculateNewCost(stock, cost, tx.getQuantity(), price);
      }
      stock = stock.add(tx.getQuantity());
    }
    return new ReplayResult(stock, cost);
  }

  public record ReplayResult(BigDecimal stock, BigDecimal cost) {}
}
