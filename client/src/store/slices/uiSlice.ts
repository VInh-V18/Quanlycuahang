import { createSlice, type PayloadAction } from "@reduxjs/toolkit";

export type Theme = "light" | "dark";

export interface UiState {
  theme: Theme;
  sidebarCollapsed: boolean;
}

const initialState: UiState = {
  theme: "light",
  sidebarCollapsed: false,
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
  },
});

export const { setTheme, toggleSidebar } = uiSlice.actions;
export default uiSlice.reducer;
