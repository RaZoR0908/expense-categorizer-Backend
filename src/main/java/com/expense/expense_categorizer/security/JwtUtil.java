package com.expense.expense_categorizer.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Base64;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration:86400000}")
    private long jwtExpiration;

    private Key getSigningKey() {
        byte[] keyBytes = Base64.getDecoder().decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public String generateToken(String email) {
        return Jwts.builder()
                .subject(email)                          // ✅ new API
                .issuedAt(new Date())                    // ✅ new API
                .expiration(new Date(System.currentTimeMillis() + jwtExpiration)) // ✅ new API
                .signWith(getSigningKey())               // ✅ new API
                .compact();
    }

    public String extractEmail(String token) {
        return Jwts.parser()                             // ✅ new API
                .verifyWith((javax.crypto.SecretKey) getSigningKey())
                .build()
                .parseSignedClaims(token)                // ✅ new API
                .getPayload()
                .getSubject();
    }

    public boolean isTokenValid(String token) {
        try {
            Jwts.parser()                                // ✅ new API
                    .verifyWith((javax.crypto.SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isTokenExpired(String token) {
        try {
            Date expiration = Jwts.parser()             // ✅ new API
                    .verifyWith((javax.crypto.SecretKey) getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload()
                    .getExpiration();
            return expiration.before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
}