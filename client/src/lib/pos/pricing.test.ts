import { describe, expect, it } from "vitest";
import { calculatePricing, clampEditablePrice, type PricingLineInput } from "@/lib/pos/pricing";

/**
 * Doi chieu voi server/src/test/java/.../pricing/OrderPricingServiceTest.java (Java, BigDecimal) -
 * calculatePricing() la ban port TypeScript CHINH XAC tung buoc cua OrderPricingService.java
 * (xem Javadoc dau file pricing.ts), nen phai cho ra dung cac ket qua da tinh tay trong file Java
 * do voi CUNG mot bo so lieu (Prompt #1, P0 test coverage).
 */
describe("calculatePricing", () => {
  it("tinh dung 3 dong voi CK dong + CK don + voucher + VAT gom gia (khop OrderPricingServiceTest)", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 100_000, quantity: 2, lineDiscountAmount: 10_000, vatRate: 10 },
      { productId: 2, unitPrice: 50_000, quantity: 3, lineDiscountAmount: 0, vatRate: 8 },
      { productId: 3, unitPrice: 200_000, quantity: 1, lineDiscountAmount: 20_000, vatRate: 5 },
    ];

    const result = calculatePricing({
      lines,
      orderDiscountAmount: 15_000,
      voucherAmount: 30_000,
      priceIncludesVat: true,
      roundingUnit: 1000,
      cashReceived: 700_000,
    });

    expect(result.subtotal).toBe(520_000);
    expect(result.totalDiscount).toBe(45_000);

    expect(result.lines[0].lineTotal).toBe(173_558);
    expect(result.lines[1].lineTotal).toBe(137_019);
    expect(result.lines[2].lineTotal).toBe(164_423);

    expect(result.lines[0].vatAmount).toBe(15_778);
    expect(result.lines[1].vatAmount).toBe(10_150);
    expect(result.lines[2].vatAmount).toBe(7_830);
    expect(result.totalVat).toBe(33_758);

    const sumLineTotals = result.lines.reduce((sum, l) => sum + l.lineTotal, 0);
    expect(sumLineTotals).toBe(result.subtotal - result.totalDiscount);

    expect(result.roundingAdjustment).toBe(0);
    expect(result.totalAmount).toBe(475_000);
    expect(result.changeAmount).toBe(225_000);
  });

  it("cong them VAT khi gia CHUA gom VAT (priceIncludesVat=false)", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 100_000, quantity: 1, lineDiscountAmount: 0, vatRate: 10 },
    ];
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 0,
      voucherAmount: 0,
      priceIncludesVat: false,
      roundingUnit: 1000,
      cashReceived: 200_000,
    });

    expect(result.lines[0].vatAmount).toBe(10_000);
    expect(result.lines[0].lineTotal).toBe(110_000);
    expect(result.totalAmount).toBe(110_000);
    expect(result.changeAmount).toBe(90_000);
  });

  it("lam tron ve don vi gan nhat (33.333 -> 33.000 voi don vi 1000)", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 33_333, quantity: 1, lineDiscountAmount: 0, vatRate: 0 },
    ];
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 0,
      voucherAmount: 0,
      priceIncludesVat: true,
      roundingUnit: 1000,
      cashReceived: null,
    });

    expect(result.totalAmount).toBe(33_000);
    expect(result.roundingAdjustment).toBe(-333);
    expect(result.changeAmount).toBeNull();
  });

  it("CK dong bang dung gia dong -> sau_CK_dong = 0, VAT tren 0 cung = 0", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 50_000, quantity: 1, lineDiscountAmount: 50_000, vatRate: 10 },
    ];
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 0,
      voucherAmount: 0,
      priceIncludesVat: true,
      roundingUnit: 1000,
      cashReceived: null,
    });

    expect(result.subtotal).toBe(0);
    expect(result.lines[0].lineTotal).toBe(0);
    expect(result.lines[0].vatAmount).toBe(0);
    expect(result.totalAmount).toBe(0);
  });

  it("2 dong ty le 300/700, CK don 100 -> phan bo 30/70, dong cuoi nhan phan du", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 300, quantity: 1, lineDiscountAmount: 0, vatRate: 0 },
      { productId: 2, unitPrice: 700, quantity: 1, lineDiscountAmount: 0, vatRate: 0 },
    ];
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 100,
      voucherAmount: 0,
      priceIncludesVat: true,
      roundingUnit: 1000,
      cashReceived: null,
    });

    expect(result.lines[0].lineTotal).toBe(270);
    expect(result.lines[1].lineTotal).toBe(630);
    expect(result.totalDiscount).toBe(100);
  });

  it("voucher hoat dong giong het CK don trong cach phan bo (khong phan biet nguon giam gia)", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 300, quantity: 1, lineDiscountAmount: 0, vatRate: 0 },
      { productId: 2, unitPrice: 700, quantity: 1, lineDiscountAmount: 0, vatRate: 0 },
    ];
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 0,
      voucherAmount: 100,
      priceIncludesVat: true,
      roundingUnit: 1000,
      cashReceived: null,
    });

    expect(result.lines[0].lineTotal).toBe(270);
    expect(result.lines[1].lineTotal).toBe(630);
    expect(result.totalDiscount).toBe(100);
  });

  it("5 dong CK don+voucher phan bo theo ty trong, tong chiet khau tung dong khop tuyet doi", () => {
    const lines: PricingLineInput[] = [1000, 2000, 3000, 4000, 5000].map((price, i) => ({
      productId: i + 1,
      unitPrice: price,
      quantity: 1,
      lineDiscountAmount: 0,
      vatRate: 0,
    }));
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 1000,
      voucherAmount: 500,
      priceIncludesVat: true,
      roundingUnit: 1,
      cashReceived: null,
    });

    expect(result.lines.map((l) => l.lineTotal)).toEqual([900, 1800, 2700, 3600, 4500]);
    const sumDiscounts = result.lines.reduce((sum, l) => sum + l.discountAmount, 0);
    expect(sumDiscounts).toBe(1500);
    expect(result.totalAmount).toBe(13_500);
  });

  it("so luong 0 (vd dong huy giua chung) khong duoc nem loi - moi so tien deu ve 0", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 1000, quantity: 0, lineDiscountAmount: 0, vatRate: 10 },
    ];
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 0,
      voucherAmount: 0,
      priceIncludesVat: true,
      roundingUnit: 1000,
      cashReceived: null,
    });

    expect(result.subtotal).toBe(0);
    expect(result.lines[0].lineTotal).toBe(0);
    expect(result.lines[0].vatAmount).toBe(0);
    expect(result.totalAmount).toBe(0);
  });

  it("tien khach dua it hon tong tien -> tien thua am (chua thu du)", () => {
    const lines: PricingLineInput[] = [
      { productId: 1, unitPrice: 100_000, quantity: 1, lineDiscountAmount: 0, vatRate: 0 },
    ];
    const result = calculatePricing({
      lines,
      orderDiscountAmount: 0,
      voucherAmount: 0,
      priceIncludesVat: true,
      roundingUnit: 1000,
      cashReceived: 50_000,
    });

    expect(result.changeAmount).toBe(-50_000);
  });
});

/**
 * Gia POS cho sua tay khi thu ngan thuong luong voi khach (VD PosPage.tsx updatePrice()) - phai
 * kep trong [0, catalogPrice], khong bao gio duoc VUOT gia niem yet (Backend chi nhan chiet khau
 * >=0, khong nhan phu thu qua lineDiscountAmount).
 */
describe("clampEditablePrice", () => {
  it("giu nguyen gia trong khoang hop le", () => {
    expect(clampEditablePrice(80_000, 100_000)).toBe(80_000);
  });

  it("kep ve catalogPrice khi nhap cao hon gia niem yet", () => {
    expect(clampEditablePrice(150_000, 100_000)).toBe(100_000);
  });

  it("kep ve 0 khi nhap gia am", () => {
    expect(clampEditablePrice(-10_000, 100_000)).toBe(0);
  });

  it("kep ve 0 khi gia tri khong hop le (NaN/0/falsy)", () => {
    expect(clampEditablePrice(Number.NaN, 100_000)).toBe(0);
    expect(clampEditablePrice(0, 100_000)).toBe(0);
  });

  it("cho phep giam gia ve dung 0 (mien phi)", () => {
    expect(clampEditablePrice(0, 100_000)).toBe(0);
  });
});
