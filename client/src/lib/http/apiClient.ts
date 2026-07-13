import axios from "axios";
import { store } from "@/store";
import { clearCredentials, setAccessToken } from "@/store/slices/authSlice";
import { registerAuthRefreshInterceptor } from "@/lib/http/authRefreshInterceptor";

/** access token giữ ở Redux (RAM, không localStorage — D3); refresh token nằm trong
 * httpOnly cookie do Backend set, gửi tự động qua withCredentials. */
export const apiClient = axios.create({
  baseURL: "/api/v1",
  withCredentials: true,
});

apiClient.interceptors.request.use((config) => {
  const token = store.getState().auth.accessToken;
  if (token) {
    config.headers.set("Authorization", `Bearer ${token}`);
  }
  return config;
});

registerAuthRefreshInterceptor({
  client: apiClient,
  getToken: () => store.getState().auth.accessToken,
  setToken: (token) => {
    if (token) {
      store.dispatch(setAccessToken(token));
    } else {
      store.dispatch(clearCredentials());
    }
  },
  refreshUrl: "/auth/refresh",
});
