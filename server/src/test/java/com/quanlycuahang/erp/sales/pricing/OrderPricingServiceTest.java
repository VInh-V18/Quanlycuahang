package com.quanlycuahang.erp.sales.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit test thuan JUnit 5, KHONG Spring context (Phase 8 Gate): kich ban ban 3 SP + CK dong + CK
 * don + voucher + tien thua phai khop tinh tay tung dong (xem phep tinh tay trong Javadoc tung
 * test).
 */
class OrderPricingServiceTest {

  /**
   * SP1: 100.000 x2, CK dong 10.000, VAT 10% -> sau_CK_dong=190.000 SP2: 50.000 x3, CK dong 0, VAT
   * 8% -> sau_CK_dong=150.000 SP3: 200.000 x1, CK dong 20.000, VAT 5% -> sau_CK_dong=180.000
   * tong_hang = 520.000; CK don 15.000 + voucher 30.000 = 45.000 Phan bo ty trong: SP1=16.442,
   * SP2=12.981, SP3=15.577 (du vao dong cuoi) sau_CK_don: SP1=173.558, SP2=137.019, SP3=164.423 VAT
   * (gia gom VAT, boc tach): SP1=173.558*10/110=15.778, SP2=137.019*8/108=10.150
   * (10149,55->HALF_UP), SP3=164.423*5/105=7.830 (7829,67->HALF_UP) -> tong=33.758 tong_thanh_toan
   * truoc lam tron = 475.000 (da la boi so 1000 -> lam tron = 0) tien_khach_dua 700.000 ->
   * tien_thua = 225.000
   */
  @Test
  void calculatesThreeLineOrderWithLineDiscountOrderDiscountAndVoucherExactly() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(100_000), bd(2), bd(10_000), bd(10)),
            new OrderLineInput(2L, bd(50_000), bd(3), BigDecimal.ZERO, bd(8)),
            new OrderLineInput(3L, bd(200_000), bd(1), bd(20_000), bd(5)));

    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(15_000), bd(30_000), true, bd(1000), bd(700_000));

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getSubtotalAmount()).isEqualByComparingTo(bd(520_000));
    assertThat(result.getDiscountAmount()).isEqualByComparingTo(bd(45_000));

    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(173_558));
    assertThat(result.getLines().get(1).getLineTotal()).isEqualByComparingTo(bd(137_019));
    assertThat(result.getLines().get(2).getLineTotal()).isEqualByComparingTo(bd(164_423));

    assertThat(result.getLines().get(0).getVatAmount()).isEqualByComparingTo(bd(15_778));
    assertThat(result.getLines().get(1).getVatAmount()).isEqualByComparingTo(bd(10_150));
    assertThat(result.getLines().get(2).getVatAmount()).isEqualByComparingTo(bd(7_830));
    assertThat(result.getVatAmount()).isEqualByComparingTo(bd(33_758));

    // Tong 3 dong phai khop tuyet doi voi subtotal - discount (bat buoc, khong lech 1 dong).
    BigDecimal sumLineTotals =
        result.getLines().stream()
            .map(OrderLineResult::getLineTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sumLineTotals)
        .isEqualByComparingTo(result.getSubtotalAmount().subtract(result.getDiscountAmount()));

    assertThat(result.getRoundingAdjustment()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(475_000));
    assertThat(result.getChangeAmount()).isEqualByComparingTo(bd(225_000));
  }

  @Test
  void appliesVatOnTopWhenPriceExcludesVat() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(100_000), bd(1), BigDecimal.ZERO, bd(10)));
    OrderPricingRequest request =
        new OrderPricingRequest(
            lines, BigDecimal.ZERO, BigDecimal.ZERO, false, bd(1000), bd(200_000));

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getVatAmount()).isEqualByComparingTo(bd(10_000));
    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(110_000));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(110_000));
    assertThat(result.getChangeAmount()).isEqualByComparingTo(bd(90_000));
  }

  @Test
  void roundsTotalToNearestConfiguredUnit() {
    // 1 dong 33.333 x1, khong CK/VAT, lam tron don vi 1000 -> 33.000 (33.333 gan 33.000 hon 34.000)
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(33_333), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(33_000));
    assertThat(result.getRoundingAdjustment()).isEqualByComparingTo(bd(-333));
    assertThat(result.getChangeAmount()).isNull();
  }

  private static BigDecimal bd(long value) {
    return BigDecimal.valueOf(value);
  }
}
