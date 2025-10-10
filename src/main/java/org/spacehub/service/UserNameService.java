package org.spacehub.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.function.Function;

@Service
public class UserNameService {

  private static final Logger logger = LoggerFactory.getLogger(UserNameService.class);

  // Base64-encoded secret key
  private static final String SECRET_KEY = "7de8e1761eeac40efc9314980ebd00fbd55978f497b50ffee42902bba14d0596";

  private SecretKey getSigningKey() {
    byte[] keyBytes = Base64.getDecoder().decode(SECRET_KEY);
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public String generateToken(UserDetails userDetails) {
    return generateToken(Map.of(), userDetails);
  }

  public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
    SecretKey key = getSigningKey();
    long nowMillis = System.currentTimeMillis();
    long expMillis = nowMillis + 1000L * 60 * 60 * 24;
    Map<String, Object> payload = Map.of(
      "username", userDetails.getUsername(),
      "iat", nowMillis / 1000,
      "exp", expMillis / 1000
    );
    if (extraClaims != null && !extraClaims.isEmpty()) {
      payload = new java.util.HashMap<>(payload);
      payload.putAll(extraClaims);
    }
    return Jwts.builder()
      .setHeaderParam("typ", "JWT")
      .setClaims(payload)
      .signWith(key)
      .compact();
  }
  public String extractUsername(String token) {
    return extractClaim(token, Claims::getSubject);
  }

  public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    try {
      Claims claims = (Claims) Jwts.parser()
        .setSigningKey(getSigningKey());
      return claimsResolver.apply(claims);
    } catch (Exception e) {
      logger.warn("Failed to extract claim from token: {}", e.getMessage());
      return null;
    }
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username != null && username.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    Date expiration = extractClaim(token, Claims::getExpiration);
    return expiration == null || expiration.before(new Date());
  }

  public boolean validToken(String token, UserDetails userDetails) {
    final String username = extractUsername(token);
    return username != null && username.equals(userDetails.getUsername());
  }
}

