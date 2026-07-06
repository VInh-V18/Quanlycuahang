import { createSlice, type PayloadAction } from "@reduxjs/toolkit";
import { decodeJwtPayload, type AccessTokenClaims } from "@/lib/jwt";
import type { AuthState } from "@/types/permission";

const initialState: AuthState = {
  user: null,
  accessToken: null,
  permissions: [],
};

const authSlice = createSlice({
  name: "auth",
  initialState,
  reducers: {
    setAccessToken(state, action: PayloadAction<string>) {
      const token = action.payload;
      state.accessToken = token;
      const claims = decodeJwtPayload<AccessTokenClaims>(token);
      if (claims) {
        state.user = { id: 0, username: claims.sub, fullName: claims.sub };
        state.permissions = claims.authorities ?? [];
      }
    },
    clearCredentials(state) {
      state.accessToken = null;
      state.user = null;
      state.permissions = [];
    },
  },
});

export const { setAccessToken, clearCredentials } = authSlice.actions;
export default authSlice.reducer;
