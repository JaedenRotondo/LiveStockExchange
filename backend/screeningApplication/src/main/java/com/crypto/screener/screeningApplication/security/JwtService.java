package com.crypto.screener.screeningApplication.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // ── Generate ─────────────────────────────────────────────────
    // Produces: { "sub": "<uuid>", "email": "...", "iat": ..., "exp": ... }
    public String generateToken(String userId, String email) {
        return Jwts.builder()
                .subject(userId)                          // sub = UUID string
                .claim("email", email)                    // extra claim
                .issuedAt(new Date())                     // iat
                .expiration(new Date(System.currentTimeMillis() + expiration)) // exp
                .signWith(getKey())
                .compact();
    }

    // ── Extract sub (userId) ─────────────────────────────────────
    public String extractUserId(String token) {
        return extractClaims(token).getSubject();         // reads "sub"
    }

    // ── Extract email ────────────────────────────────────────────
    public String extractEmail(String token) {
        return extractClaims(token).get("email", String.class);
    }

    // ── Validate ─────────────────────────────────────────────────
    public boolean isTokenValid(String token) {
        try {
            Claims claims = extractClaims(token);
            return claims.getExpiration().after(new Date());
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    // ── Internal ─────────────────────────────────────────────────
    private Claims extractClaims(String token) {
        return Jwts.parser()
                .verifyWith(getKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(hexToBytes(secret));
    }

    private byte[] hexToBytes(String hex) {
        int len = hex.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4)
                    + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }
}