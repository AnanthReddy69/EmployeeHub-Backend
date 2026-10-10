package com.anurag.ai.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
public class JWTService {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration-ms:86400000}")
    private long expirationMs;

    private SecretKey getKey() {
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);

        if (bytes.length < 32) {
            throw new IllegalStateException(
                "jwt.secret must be at least 32 bytes long."
            );
        }

        return Keys.hmacShaKeyFor(bytes);
    }

    public String generateToken(String email) {
        Date now = new Date();

        return Jwts.builder()
            .subject(email)
            .issuedAt(now)
            .expiration(new Date(now.getTime() + expirationMs))
            .signWith(getKey())
            .compact();
    }

    public String extractUsername(String token) {
        return getClaims(token).getSubject();
    }

    private Claims getClaims(String token) {
        return Jwts.parser()
            .verifyWith(getKey())
            .build()
            .parseSignedClaims(token)
            .getPayload();
    }

    public boolean validateToken(String token, UserDetails userDetails) {
        Claims claims = getClaims(token);

        return userDetails.getUsername().equals(claims.getSubject())
            && claims.getExpiration() != null
            && claims.getExpiration().after(new Date());
    }
}