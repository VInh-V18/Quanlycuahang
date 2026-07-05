package com.quanlycuahang.erp.auth.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Sinh/verify JWT (access token + refresh token) bang jjwt 0.12.x. Access token mang danh sach
 * authority de Spring Security doc truc tiep tu SecurityContext, khong can query lai DB moi
 * request.
 */
@Service
public class JwtService {

  private static final String CLAIM_AUTHORITIES = "authorities";
  private static final String CLAIM_TOKEN_FAMILY = "tokenFamily";

  private final SecretKey signingKey;
  private final long accessTokenTtlMillis;
  private final long refreshTokenTtlMillis;

  public JwtService(
      @Value("${app.jwt.secret}") String secret,
      @Value("${app.jwt.access-token-ttl-minutes}") long accessTokenTtlMinutes,
      @Value("${app.jwt.refresh-token-ttl-days}") long refreshTokenTtlDays) {
    this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenTtlMillis = accessTokenTtlMinutes * 60_000L;
    this.refreshTokenTtlMillis = refreshTokenTtlDays * 24 * 60 * 60_000L;
  }

  public String generateAccessToken(String username, List<String> authorities) {
    Date now = new Date();
    return Jwts.builder()
        .subject(username)
        .claim(CLAIM_AUTHORITIES, authorities)
        .issuedAt(now)
        .expiration(new Date(now.getTime() + accessTokenTtlMillis))
        .signWith(signingKey)
        .compact();
  }

  /** Sinh refresh token moi cho 1 tokenFamily (tao moi khi dang nhap, giu nguyen khi rotate). */
  public GeneratedRefreshToken generateRefreshToken(String username, String tokenFamily) {
    String jti = UUID.randomUUID().toString();
    Date now = new Date();
    Date expiry = new Date(now.getTime() + refreshTokenTtlMillis);
    String token =
        Jwts.builder()
            .subject(username)
            .id(jti)
            .claim(CLAIM_TOKEN_FAMILY, tokenFamily)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(signingKey)
            .compact();
    return new GeneratedRefreshToken(token, jti, tokenFamily, refreshTokenTtlMillis);
  }

  public long getRefreshTokenTtlMillis() {
    return refreshTokenTtlMillis;
  }

  /**
   * @throws JwtException neu token khong hop le/het han — Controller/Filter bat rieng.
   */
  public Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(signingKey).build().parseSignedClaims(token).getPayload();
  }

  @SuppressWarnings("unchecked")
  public List<String> extractAuthorities(Claims claims) {
    return (List<String>) claims.get(CLAIM_AUTHORITIES, List.class);
  }

  public String extractTokenFamily(Claims claims) {
    return claims.get(CLAIM_TOKEN_FAMILY, String.class);
  }

  public String extractUsername(Claims claims) {
    return claims.getSubject();
  }

  public String extractJti(Claims claims) {
    return claims.getId();
  }

  /** Ket qua sinh refresh token: token da ky + metadata de RefreshTokenService luu Redis. */
  public record GeneratedRefreshToken(
      String token, String jti, String tokenFamily, long ttlMillis) {}
}
