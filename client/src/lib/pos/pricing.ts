/**
 * Port TypeScript cua OrderPricingService.java (B4, 7 buoc) — dung de hien thi tong tien NGAY LAP
 * TUC tren POS truoc khi goi API. Backend van la nguon su that cuoi cung (tinh lai doc lap luc
 * tao don, tu choi ORDER_PRICE_MISMATCH neu lech) — file nay chi la ban sao chinh xac tung buoc de
 * khong bi lech lam tron/phan bo chiet khau so voi Java.
 */

export interface PricingLineInput {
  productId: number;
  unitPrice: number;
  quantity: number;
  lineDiscountAmount: number;
  vatRate: number;
}

export interface PricingRequest {
  lines: PricingLineInput[];
  orderDiscountAmount: number;
  voucherAmount: number;
  priceIncludesVat: boolean;
  roundingUnit: number;
  cashReceived: number | null;
}

export interface PricingLineResult {
  productId: number;
  unitPrice: number;
  quantity: number;
  discountAmount: number;
  vatAmount: number;
  lineTotal: number;
}

export interface PricingResult {
  lines: PricingLineResult[];
  subtotal: number;
  totalDiscount: number;
  totalVat: number;
  roundingAdjustment: number;
  totalAmount: number;
  changeAmount: number | null;
}

function round0(value: number): number {
  return Math.round(value);
}

export function calculatePricing(request: PricingRequest): PricingResult {
  const n = request.lines.length;
  const sauCkDong: number[] = new Array(n);
  let subtotal = 0;

  for (let i = 0; i < n; i++) {
    const line = request.lines[i];
    const thanhTienDong = round0(line.unitPrice * line.quantity);
    sauCkDong[i] = thanhTienDong - line.lineDiscountAmount;
    subtotal += sauCkDong[i];
  }

  const orderLevelReduction = request.orderDiscountAmount + request.voucherAmount;
  const allocatedReduction: number[] = new Array(n);
  let allocatedSum = 0;

  if (n > 0) {
    for (let i = 0; i < n - 1; i++) {
      if (subtotal === 0) {
        allocatedReduction[i] = 0;
      } else {
        const ratio = sauCkDong[i] / subtotal;
        allocatedReduction[i] = round0(orderLevelReduction * ratio);
      }
      allocatedSum += allocatedReduction[i];
    }
    allocatedReduction[n - 1] = orderLevelReduction - allocatedSum;
  }

  const lines: PricingLineResult[] = [];
  let totalVat = 0;
  let totalBeforeRounding = 0;

  for (let i = 0; i < n; i++) {
    const line = request.lines[i];
    const lineAfterOrderDiscount = sauCkDong[i] - allocatedReduction[i];
    let vatAmount: number;
    let lineTotal: number;

    if (request.priceIncludesVat) {
      const denom = 100 + line.vatRate;
      vatAmount = denom === 0 ? 0 : round0((lineAfterOrderDiscount * line.vatRate) / denom);
      lineTotal = lineAfterOrderDiscount;
    } else {
      vatAmount = round0((lineAfterOrderDiscount * line.vatRate) / 100);
      lineTotal = lineAfterOrderDiscount + vatAmount;
    }

    totalVat += vatAmount;
    totalBeforeRounding += lineTotal;

    lines.push({
      productId: line.productId,
      unitPrice: line.unitPrice,
      quantity: line.quantity,
      discountAmount: line.lineDiscountAmount + allocatedReduction[i],
      vatAmount,
      lineTotal,
    });
  }

  const roundedTotal = roundToNearestUnit(totalBeforeRounding, request.roundingUnit);
  const roundingAdjustment = roundedTotal - totalBeforeRounding;
  const changeAmount = request.cashReceived == null ? null : request.cashReceived - roundedTotal;

  return {
    lines,
    subtotal,
    totalDiscount: orderLevelReduction,
    totalVat,
    roundingAdjustment,
    totalAmount: roundedTotal,
    changeAmount,
  };
}

function roundToNearestUnit(amount: number, unit: number): number {
  if (!unit || unit <= 0) {
    return round0(amount);
  }
  return Math.round(amount / unit) * unit;
}

/** Kep gia ban sua tay trong POS ve [0, catalogPrice] - khong cho tang gia qua gia niem yet
 * (Backend chi nhan chiet khau >=0, khong nhan phu thu — B4). Tach ra day de test doc lap voi
 * PosPage (von can render toan bo component moi goi duoc). */
export function clampEditablePrice(rawValue: number, catalogPrice: number): number {
  return Math.min(catalogPrice, Math.max(0, rawValue || 0));
}
