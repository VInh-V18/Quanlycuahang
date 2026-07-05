package com.quanlycuahang.erp.auth.security;

/** Ket qua noi bo giua AuthService va AuthController (khong phai DTO API tra ra ngoai). */
public record AuthTokens(String accessToken, String refreshToken, long refreshTokenTtlMillis) {}
