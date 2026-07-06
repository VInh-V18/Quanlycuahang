import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

/** Giỏ hàng POS — giữ ở Redux + persist (mất mạng/refresh không mất giỏ hàng đang bán,
 * đúng B4 edge case "mất mạng giữa chừng"). Backend luôn tính lại giá trị cuối cùng
 * (OrderPricingService) khi tạo đơn — state này chỉ phục vụ hiển thị tạm thời phía FE. */
export interface CartLine {
  productId: number;
  productName: string;
  unitPrice: number;
  quantity: number;
  lineDiscountAmount: number;
}

export interface CartState {
  branchId: number | null;
  customerId: number | null;
  voucherCode: string | null;
  orderDiscountAmount: number;
  lines: CartLine[];
}

const initialState: CartState = {
  branchId: null,
  customerId: null,
  voucherCode: null,
  orderDiscountAmount: 0,
  lines: [],
};

const cartSlice = createSlice({
  name: "cart",
  initialState,
  reducers: {
    setBranch(state, action: PayloadAction<number>) {
      state.branchId = action.payload;
    },
    setCustomer(state, action: PayloadAction<number | null>) {
      state.customerId = action.payload;
    },
    setVoucherCode(state, action: PayloadAction<string | null>) {
      state.voucherCode = action.payload;
    },
    setOrderDiscountAmount(state, action: PayloadAction<number>) {
      state.orderDiscountAmount = action.payload;
    },
    addLine(state, action: PayloadAction<Omit<CartLine, "lineDiscountAmount">>) {
      const existing = state.lines.find((l) => l.productId === action.payload.productId);
      if (existing) {
        existing.quantity += action.payload.quantity;
      } else {
        state.lines.push({ ...action.payload, lineDiscountAmount: 0 });
      }
    },
    updateLineQuantity(state, action: PayloadAction<{ productId: number; quantity: number }>) {
      const line = state.lines.find((l) => l.productId === action.payload.productId);
      if (line) line.quantity = action.payload.quantity;
    },
    updateLineDiscount(
      state,
      action: PayloadAction<{ productId: number; lineDiscountAmount: number }>,
    ) {
      const line = state.lines.find((l) => l.productId === action.payload.productId);
      if (line) line.lineDiscountAmount = action.payload.lineDiscountAmount;
    },
    removeLine(state, action: PayloadAction<number>) {
      state.lines = state.lines.filter((l) => l.productId !== action.payload);
    },
    clearCart() {
      return initialState;
    },
  },
});

export const {
  setBranch,
  setCustomer,
  setVoucherCode,
  setOrderDiscountAmount,
  addLine,
  updateLineQuantity,
  updateLineDiscount,
  removeLine,
  clearCart,
} = cartSlice.actions;
export default cartSlice.reducer;
