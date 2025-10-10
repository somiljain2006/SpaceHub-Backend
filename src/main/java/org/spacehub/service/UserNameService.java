package org.spacehub.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class UserNameService {

    private static final String SECRET_KEY = "7de8e1761eeac40efc9314980ebd00fbd55978f497b50ffee42902bba14d0596";

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String generateToken(Map<String, Object> extraClaims, UserDetails userDetails) {
        long expirationTime = 1000 * 60 * 60 * 24;

        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(getSignInKey())
                .compact();
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(), userDetails);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = parseClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getSignInKey() {
        byte[] keys_array = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keys_array);
    }

    public boolean validToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (!isTokenExpired(token) && username.equals(userDetails.getUsername()));
    }

    private boolean isTokenExpired(String token) {
        return extractInformation(token).before(new Date());
    }

    private Date extractInformation(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

}
