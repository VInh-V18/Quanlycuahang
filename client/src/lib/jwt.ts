/** Giải mã payload JWT phía client chỉ để hiển thị UI (username, quyền) — KHÔNG dùng để xác thực,
 * việc xác thực chữ ký luôn do Backend đảm nhiệm. */
export function decodeJwtPayload<T>(token: string): T | null {
  try {
    const base64Url = token.split(".")[1];
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const json = decodeURIComponent(
      atob(base64)
        .split("")
        .map((c) => "%" + c.charCodeAt(0).toString(16).padStart(2, "0"))
        .join(""),
    );
    return JSON.parse(json) as T;
  } catch {
    return null;
  }
}

export interface AccessTokenClaims {
  sub: string;
  authorities: string[];
  iat: number;
  exp: number;
}
