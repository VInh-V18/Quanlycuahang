package com.quanlycuahang.erp.auth.dto;

/** Response tra ve cho FE — refresh token KHONG nam trong body, chi trong httpOnly cookie (D3). */
public class AccessTokenResponse {

  private final String accessToken;

  public AccessTokenResponse(String accessToken) {
    this.accessToken = accessToken;
  }

  public String getAccessToken() {
    return accessToken;
  }
}
