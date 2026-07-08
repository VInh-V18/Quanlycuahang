import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type Theme = "light" | "dark";

export interface UiState {
  theme: Theme;
  sidebarCollapsed: boolean;
  /** Chi nhanh dang lam viec, chon o Topbar — null nghia la chua chon (dung fallback
   * CURRENT_BRANCH_ID cho toi khi Topbar nap xong danh sach chi nhanh cua user). */
  currentBranchId: number | null;
}

const initialState: UiState = {
  theme: "light",
  sidebarCollapsed: false,
  currentBranchId: null,
};

const uiSlice = createSlice({
  name: "ui",
  initialState,
  reducers: {
    setTheme(state, action: PayloadAction<Theme>) {
      state.theme = action.payload;
    },
    toggleSidebar(state) {
      state.sidebarCollapsed = !state.sidebarCollapsed;
    },
    setCurrentBranchId(state, action: PayloadAction<number>) {
      state.currentBranchId = action.payload;
    },
  },
});

export const { setTheme, toggleSidebar, setCurrentBranchId } = uiSlice.actions;
export default uiSlice.reducer;
