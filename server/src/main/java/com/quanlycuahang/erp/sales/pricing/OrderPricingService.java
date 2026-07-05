package com.quanlycuahang.erp.sales.pricing;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Tinh tien 1 don POS dung dung 7 buoc B4 — Java thuan, KHONG phu thuoc Spring context, de unit
 * test khong can khoi dong ApplicationContext (Phase 11). Duoc goi doc lap ca o Frontend (mo phong
 * de hien thi ngay lap tuc) lan Backend (nguon chan ly cuoi cung khi tao don — B4 UC-04).
 *
 * <p>1. thanh_tien_dong = don_gia x so_luong 2. sau_CK_dong = thanh_tien_dong - chiet_khau_dong 3.
 * tong_hang = Sigma sau_CK_dong 4. sau_CK_don = tong_hang - chiet_khau_don - voucher (phan bo nguoc
 * ve tung dong theo ty trong, de tinh lai gop dung) 5. VAT tren sau_CK_don da phan bo ve dong: boc
 * tach neu gia gom VAT, cong them neu gia chua gom 6. tong_thanh_toan = sau_CK_don (+ Sigma VAT neu
 * gia chua gom) +/- lam_tron 7. tien_thua = tien_khach_dua - tong_thanh_toan
 *
 * <p>Phan bo CK don/voucher va lam tron deu dung so du (residual) don vao dong cuoi cung de dam bao
 * tong cac dong khop chinh xac header — khong bao gio lech 1 dong do sai so chia lam tron.
 */
public final class OrderPricingService {

  private OrderPricingService() {}

  public static OrderPricingResult calculate(OrderPricingRequest request) {
    List<OrderLineInput> inputs = request.getLines();
    int n = inputs.size();

    BigDecimal[] sauCkDong = new BigDecimal[n];
    BigDecimal subtotal = BigDecimal.ZERO;

    for (int i = 0; i < n; i++) {
      OrderLineInput line = inputs.get(i);
      BigDecimal thanhTienDong =
          line.getUnitPrice().multiply(line.getQuantity()).setScale(0, RoundingMode.HALF_UP);
      sauCkDong[i] = thanhTienDong.subtract(line.getLineDiscountAmount());
      subtotal = subtotal.add(sauCkDong[i]);
    }

    BigDecimal orderLevelReduction =
        request.getOrderDiscountAmount().add(request.getVoucherAmount());

    BigDecimal[] allocatedReduction = new BigDecimal[n];
    BigDecimal allocatedSum = BigDecimal.ZERO;
    if (n > 0) {
      for (int i = 0; i < n - 1; i++) {
        if (subtotal.compareTo(BigDecimal.ZERO) == 0) {
          allocatedReduction[i] = BigDecimal.ZERO;
        } else {
          BigDecimal ratio = sauCkDong[i].divide(subtotal, MathContext.DECIMAL64);
          allocatedReduction[i] =
              orderLevelReduction.multiply(ratio).setScale(0, RoundingMode.HALF_UP);
        }
        allocatedSum = allocatedSum.add(allocatedReduction[i]);
      }
      // Dong cuoi nhan phan con lai — dam bao tong phan bo == orderLevelReduction chinh xac.
      allocatedReduction[n - 1] = orderLevelReduction.subtract(allocatedSum);
    }

    BigDecimal[] lineAfterOrderDiscount = new BigDecimal[n];
    BigDecimal[] vatAmounts = new BigDecimal[n];
    BigDecimal[] lineTotals = new BigDecimal[n];
    BigDecimal totalVat = BigDecimal.ZERO;
    BigDecimal totalBeforeRounding = BigDecimal.ZERO;

    List<OrderLineResult> lineResults = new ArrayList<>(n);
    for (int i = 0; i < n; i++) {
      OrderLineInput line = inputs.get(i);
      lineAfterOrderDiscount[i] = sauCkDong[i].subtract(allocatedReduction[i]);

      BigDecimal rate = line.getVatRate();
      if (request.isPriceIncludesVat()) {
        BigDecimal denom = BigDecimal.valueOf(100).add(rate);
        vatAmounts[i] =
            denom.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : lineAfterOrderDiscount[i]
                    .multiply(rate)
                    .divide(denom, MathContext.DECIMAL64)
                    .setScale(0, RoundingMode.HALF_UP);
        lineTotals[i] = lineAfterOrderDiscount[i];
      } else {
        vatAmounts[i] =
            lineAfterOrderDiscount[i]
                .multiply(rate)
                .divide(BigDecimal.valueOf(100), MathContext.DECIMAL64)
                .setScale(0, RoundingMode.HALF_UP);
        lineTotals[i] = lineAfterOrderDiscount[i].add(vatAmounts[i]);
      }

      totalVat = totalVat.add(vatAmounts[i]);
      totalBeforeRounding = totalBeforeRounding.add(lineTotals[i]);

      BigDecimal lineDiscountTotal = line.getLineDiscountAmount().add(allocatedReduction[i]);
      lineResults.add(
          new OrderLineResult(
              line.getProductId(),
              line.getUnitPrice(),
              line.getQuantity(),
              lineDiscountTotal,
              vatAmounts[i],
              lineTotals[i]));
    }

    BigDecimal roundedTotal = roundToNearestUnit(totalBeforeRounding, request.getRoundingUnit());
    BigDecimal roundingAdjustment = roundedTotal.subtract(totalBeforeRounding);

    BigDecimal changeAmount =
        request.getCashReceived() == null ? null : request.getCashReceived().subtract(roundedTotal);

    return new OrderPricingResult(
        lineResults,
        subtotal,
        orderLevelReduction,
        totalVat,
        roundingAdjustment,
        roundedTotal,
        changeAmount);
  }

  private static BigDecimal roundToNearestUnit(BigDecimal amount, BigDecimal unit) {
    if (unit == null || unit.compareTo(BigDecimal.ZERO) <= 0) {
      return amount.setScale(0, RoundingMode.HALF_UP);
    }
    BigDecimal divided = amount.divide(unit, 0, RoundingMode.HALF_UP);
    return divided.multiply(unit);
  }
}
