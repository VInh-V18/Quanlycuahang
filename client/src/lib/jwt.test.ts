import { describe, expect, it } from "vitest";
import { decodeJwtPayload } from "@/lib/jwt";

function base64url(input: string) {
  return btoa(input).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

describe("decodeJwtPayload", () => {
  it("decodes the payload segment of a JWT", () => {
    const payload = { sub: "owner01", authorities: ["product:view"], iat: 1, exp: 2 };
    const token = `${base64url(JSON.stringify({ alg: "HS512" }))}.${base64url(JSON.stringify(payload))}.signature`;
    expect(decodeJwtPayload(token)).toEqual(payload);
  });

  it("returns null for a malformed token instead of throwing", () => {
    expect(decodeJwtPayload("not-a-jwt")).toBeNull();
  });
});
