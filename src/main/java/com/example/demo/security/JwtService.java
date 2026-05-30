package com.example.demo.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

@Service
public class JwtService {

    private final SecretKey key;
    private final long expiryMs;

    public JwtService(
            @Value("${app.jwt.secret:MeTrangDoAnTotNghiepSecretKey2026MustBeLongEnough}") String secret,
            @Value("${app.jwt.expiry-hours:24}") long expiryHours) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMs = expiryHours * 60 * 60 * 1000;
    }

    public String generateToken(UUID userId, UUID customerId, String role, String username) {
        var builder = Jwts.builder()
                .subject(userId.toString())
                .claim("role", role)
                .claim("username", username)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key);

        if (customerId != null) {
            builder.claim("customerId", customerId.toString());
        }
        return builder.compact();
    }

    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UUID getUserId(Claims claims) {
        return UUID.fromString(claims.getSubject());
    }

    public UUID getCustomerId(Claims claims) {
        String raw = claims.get("customerId", String.class);
        return raw != null ? UUID.fromString(raw) : null;
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }
}
