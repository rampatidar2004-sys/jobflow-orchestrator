package com.jobflow.orchestrator.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/** Issues and validates HS256 JWTs carrying the username and role as claims. */
@Service
@EnableConfigurationProperties(JwtProperties.class)
public class JwtService {

    private final JwtProperties properties;
    private final SecretKey key;

    public JwtService(JwtProperties properties) {
        this.properties = properties;
        byte[] keyBytes = properties.getJwtSecret().getBytes(StandardCharsets.UTF_8);
        this.key = Keys.hmacShaKeyFor(padTo32Bytes(keyBytes));
    }

    private byte[] padTo32Bytes(byte[] input) {
        if (input.length >= 32) {
            return input;
        }
        byte[] padded = new byte[32];
        System.arraycopy(input, 0, padded, 0, input.length);
        return padded;
    }

    public String issueToken(String username, String role) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(properties.getJwtExpirationMinutes() * 60);
        return Jwts.builder()
                .subject(username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(key)
                .compact();
    }

    public Claims parseAndValidate(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
