package org.spacehub.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.function.Function;

@Service
public class UserNameService {

  private static final String SECRET_KEY = "7de8e1761eeac40efc9314980ebd00fbd55978f497b50ffee42902bba14d0596";

  private SecretKey getSigningKey() {
    byte[] keyBytes = SECRET_KEY.getBytes(StandardCharsets.UTF_8);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(UserDetails userDetails) {

    long nowMillis = System.currentTimeMillis();
    long expMillis = nowMillis + 1000L * 60 * 60 * 24; // 24h

    return Jwts.builder()
      .claim("sub", userDetails.getUsername())
      .claim("iat", nowMillis / 1000L)
      .claim("exp", expMillis / 1000L)
      .signWith(getSigningKey())
      .compact();

  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    Claims claims = Jwts.parser()
      .verifyWith(getSigningKey())
      .build()
      .parseSignedClaims(token)
      .getPayload();
    return claimsResolver.apply(claims);
  }

  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public boolean validToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    Date expiration = extractClaim(token, Claims::getExpiration);
    return expiration.before(new Date());
  }
}
