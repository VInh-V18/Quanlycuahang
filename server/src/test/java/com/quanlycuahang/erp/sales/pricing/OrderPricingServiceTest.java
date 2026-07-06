package com.quanlycuahang.erp.sales.pricing;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Unit test thuan JUnit 5, KHONG Spring context (Phase 8/11 Gate): >=20 kich ban kiem tra tung
 * buoc trong 7 buoc B4 rieng le va ket hop, kem phep tinh tay tung dong trong Javadoc tung test.
 * Mot so kich ban (giam gia vuot subtotal, so luong = 0) mo ta hanh vi thuc te cua ham thuan —
 * khong tu clamp/validate, viec kiem tra hop le thuoc ve OrderService/FE goi no.
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

  @Test
  void singleLineNoDiscountNoVatTotalsExactly() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(50_000), bd(2), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getSubtotalAmount()).isEqualByComparingTo(bd(100_000));
    assertThat(result.getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getVatAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(0).getDiscountAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(100_000));
  }

  /** CK dong = dung gia tri dong -> sau_CK_dong = 0, VAT tren 0 cung = 0. */
  @Test
  void lineDiscountCanReduceLineAmountToZero() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(50_000), bd(1), bd(50_000), bd(10)));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getSubtotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(0).getVatAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(0).getDiscountAmount()).isEqualByComparingTo(bd(50_000));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /**
   * Ca 2 dong bi CK dong xoa het -> subtotal=0. CK don 50 van phai phan bo ma khong chia cho 0:
   * dong dau (guard subtotal==0 -> allocated=0), dong cuoi nhan het phan du (residual=50) ->
   * lineTotal am — dung boi OrderService validate truoc khi luu, ham thuan chi dam bao khong nem
   * ArithmeticException va tong luon khop tuyet doi.
   */
  @Test
  void zeroSubtotalDoesNotThrowWhenOrderLevelReductionApplied() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(100), bd(1), bd(100), BigDecimal.ZERO),
            new OrderLineInput(2L, bd(200), bd(1), bd(200), BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(50), BigDecimal.ZERO, true, bd(1), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getSubtotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getDiscountAmount()).isEqualByComparingTo(bd(50));
    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(1).getLineTotal()).isEqualByComparingTo(bd(-50));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(-50));
  }

  /** 2 dong 300/700 (ty le 30/70), CK don 100 -> dong1=30, dong2(cuoi)=70 (du). */
  @Test
  void orderDiscountAloneAllocatesProportionallyWithResidualOnLastLine() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(300), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(2L, bd(700), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(100), BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(270));
    assertThat(result.getLines().get(1).getLineTotal()).isEqualByComparingTo(bd(630));
    assertThat(result.getDiscountAmount()).isEqualByComparingTo(bd(100));
  }

  /** Dung so lieu het test tren nhung dung voucher thay CK don — ket qua phai giong het vi
   * orderLevelReduction = CK don + voucher, khong phan biet nguon. */
  @Test
  void voucherAloneBehavesSameAsOrderDiscountInAllocation() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(300), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(2L, bd(700), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, bd(100), true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(270));
    assertThat(result.getLines().get(1).getLineTotal()).isEqualByComparingTo(bd(630));
    assertThat(result.getDiscountAmount()).isEqualByComparingTo(bd(100));
  }

  /** Chi 1 dong -> vong lap phan bo theo ty trong (i < n-1) khong chay, toan bo CK don + voucher
   * do vao dong duy nhat qua nhanh "du" (residual). */
  @Test
  void combinedOrderDiscountAndVoucherOnSingleLineGoesEntirelyToResidual() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(1000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(50), bd(30), true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(920));
    assertThat(result.getDiscountAmount()).isEqualByComparingTo(bd(80));
  }

  /** 3 dong VAT khac nhau (0%/5%/10%), gia chua gom VAT -> moi dong tinh doc lap, tong VAT =
   * 0+50+100=150. */
  @Test
  void mixedVatRatesAcrossLinesSumIndependently() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(1000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(2L, bd(1000), bd(1), BigDecimal.ZERO, bd(5)),
            new OrderLineInput(3L, bd(1000), bd(1), BigDecimal.ZERO, bd(10)));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, false, bd(1), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getVatAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(1).getVatAmount()).isEqualByComparingTo(bd(50));
    assertThat(result.getLines().get(2).getVatAmount()).isEqualByComparingTo(bd(100));
    assertThat(result.getVatAmount()).isEqualByComparingTo(bd(150));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(3150));
  }

  @Test
  void zeroVatRateProducesZeroVatAmount() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(100), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, false, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getVatAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(100));
  }

  /** So luong le (0.333) — thanh_tien_dong = 33.333 x 0,333 = 11.099,889 -> HALF_UP -> 11.100. */
  @Test
  void fractionalQuantityRoundsLineAmountHalfUp() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(33_333), bdStr("0.333"), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(11_100));
    assertThat(result.getSubtotalAmount()).isEqualByComparingTo(bd(11_100));
  }

  /** 1.250 truoc lam tron, don vi lam tron 500 -> 1.250/500=2,5 -> HALF_UP -> 3 -> 1.500. */
  @Test
  void roundingUnitOf500RoundsUpAtMidpoint() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(1250), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(500), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(1500));
    assertThat(result.getRoundingAdjustment()).isEqualByComparingTo(bd(250));
  }

  /** 1.240 truoc lam tron, don vi 100 -> 1.240/100=12,4 -> HALF_UP -> 12 -> 1.200 (lam tron
   * xuong vi duoi diem giua). */
  @Test
  void roundingUnitOf100RoundsDownWhenBelowMidpoint() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(1240), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(100), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(1200));
    assertThat(result.getRoundingAdjustment()).isEqualByComparingTo(bd(-40));
  }

  /** roundingUnit=null -> OrderPricingRequest tu quy ve BigDecimal.ONE -> khong gom nhom, chi
   * xac nhan lai gia tri nguyen san co (khong phat sinh chenh lech lam tron). */
  @Test
  void nullRoundingUnitDefaultsToOneAndPerformsNoBucketing() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(1000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, null, null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(1000));
    assertThat(result.getRoundingAdjustment()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /**
   * 5 dong 1000/2000/3000/4000/5000 (subtotal=15.000), CK don 1000 + voucher 500 = 1500. Ca 4 ty
   * le dau chia het (100/200/300/400), dong cuoi nhan du (500) — kiem tra khong lech tong dong nao
   * du co nhieu dong.
   */
  @Test
  void residualAllocationAcrossFiveLinesSumsExactlyToReduction() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(1000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(2L, bd(2000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(3L, bd(3000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(4L, bd(4000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(5L, bd(5000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(1000), bd(500), true, bd(1), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(900));
    assertThat(result.getLines().get(1).getLineTotal()).isEqualByComparingTo(bd(1800));
    assertThat(result.getLines().get(2).getLineTotal()).isEqualByComparingTo(bd(2700));
    assertThat(result.getLines().get(3).getLineTotal()).isEqualByComparingTo(bd(3600));
    assertThat(result.getLines().get(4).getLineTotal()).isEqualByComparingTo(bd(4500));

    BigDecimal sumDiscounts =
        result.getLines().stream()
            .map(OrderLineResult::getDiscountAmount)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    assertThat(sumDiscounts).isEqualByComparingTo(bd(1500));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(13_500));
  }

  @Test
  void negativeChangeWhenCashReceivedLessThanTotal() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(100_000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1000), bd(50_000));

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getChangeAmount()).isEqualByComparingTo(bd(-50_000));
  }

  @Test
  void changeAmountNullWhenCashReceivedNotProvided() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(20_000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getChangeAmount()).isNull();
  }

  /**
   * dong0: 1000 x1, CK dong 100 -> sau_CK_dong=900; dong1: 1000 x1, khong CK dong -> 1000.
   * subtotal=1900, CK don 190 -> ty trong dong0=900/1900*190=90 (chia het), dong1(cuoi)=100.
   * discountAmount tung dong PHAI cong ca CK dong rieng le lan phan bo CK don.
   */
  @Test
  void lineResultDiscountAmountCombinesLineAndAllocatedOrderDiscount() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(1000), bd(1), bd(100), BigDecimal.ZERO),
            new OrderLineInput(2L, bd(1000), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(190), BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getDiscountAmount()).isEqualByComparingTo(bd(190));
    assertThat(result.getLines().get(1).getDiscountAmount()).isEqualByComparingTo(bd(100));
    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(810));
    assertThat(result.getLines().get(1).getLineTotal()).isEqualByComparingTo(bd(900));
  }

  /** CK don (150) vuot ca subtotal cua dong duy nhat (100) -> lineTotal am. Ham thuan khong tu
   * validate/clamp — OrderService phai chan truong hop nay truoc khi goi. */
  @Test
  void orderDiscountExceedingLineSubtotalProducesNegativeLineTotal() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(100), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(150), BigDecimal.ZERO, true, bd(1), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(-50));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(-50));
  }

  @Test
  void allZeroVatRatesAcrossMultipleLinesSumsToZero() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(100), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(2L, bd(200), bd(1), BigDecimal.ZERO, BigDecimal.ZERO),
            new OrderLineInput(3L, bd(300), bd(1), BigDecimal.ZERO, BigDecimal.ZERO));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getVatAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    result.getLines().forEach(line -> assertThat(line.getVatAmount()).isEqualByComparingTo(BigDecimal.ZERO));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(600));
  }

  /** So luong 0 (vd don huy giua chung) khong duoc nem exception — moi so tien deu ve 0. */
  @Test
  void lineWithZeroQuantityProducesZeroAmounts() {
    List<OrderLineInput> lines =
        List.of(new OrderLineInput(1L, bd(1000), BigDecimal.ZERO, BigDecimal.ZERO, bd(10)));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, BigDecimal.ZERO, BigDecimal.ZERO, true, bd(1000), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getSubtotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getLines().get(0).getVatAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    assertThat(result.getTotalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
  }

  /**
   * Ket hop CK dong + CK don + VAT cong them tren 2 dong: dong0 1000x1 CK dong 100 -> 900; dong1
   * 2000x1 khong CK dong. subtotal=2900, CK don 290 -> ty trong dong0=900/2900*290=90 (chia het),
   * dong1(cuoi)=200. sau_CK_don: dong0=810,dong1=1800. VAT 10% cong them: dong0=81->891,
   * dong1=180->1980. Tong VAT=261, tong truoc lam tron=2871.
   */
  @Test
  void priceExcludesVatWithLineAndOrderDiscountCombined() {
    List<OrderLineInput> lines =
        List.of(
            new OrderLineInput(1L, bd(1000), bd(1), bd(100), bd(10)),
            new OrderLineInput(2L, bd(2000), bd(1), BigDecimal.ZERO, bd(10)));
    OrderPricingRequest request =
        new OrderPricingRequest(lines, bd(290), BigDecimal.ZERO, false, bd(1), null);

    OrderPricingResult result = OrderPricingService.calculate(request);

    assertThat(result.getLines().get(0).getVatAmount()).isEqualByComparingTo(bd(81));
    assertThat(result.getLines().get(0).getLineTotal()).isEqualByComparingTo(bd(891));
    assertThat(result.getLines().get(1).getVatAmount()).isEqualByComparingTo(bd(180));
    assertThat(result.getLines().get(1).getLineTotal()).isEqualByComparingTo(bd(1980));
    assertThat(result.getVatAmount()).isEqualByComparingTo(bd(261));
    assertThat(result.getTotalAmount()).isEqualByComparingTo(bd(2871));
  }

  private static BigDecimal bd(long value) {
    return BigDecimal.valueOf(value);
  }

  private static BigDecimal bdStr(String value) {
    return new BigDecimal(value);
  }
}
