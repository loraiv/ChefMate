package com.chefmate.backend.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Service
public class JwtService {

    private static final Logger logger = LoggerFactory.getLogger(JwtService.class);

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private Long expiration;

    private SecretKey getSigningKey() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException(
                "JWT secret is not configured. " +
                "Please set JWT_SECRET environment variable with a secure random string."
            );
        }
        
        // For development: allow default secret but warn
        boolean isDefaultSecret = secret.equals("your-super-secret-jwt-key-change-this-in-production-12345");
        if (isDefaultSecret) {
            System.err.println("WARNING: Using default JWT secret! This is insecure for production.");
            System.err.println("Please set JWT_SECRET environment variable with a secure random string.");
        }
        
        // Ensure minimum length for security (HS256 requires at least 256 bits = 32 bytes)
        if (secret.length() < 32) {
            throw new IllegalStateException(
                "JWT secret must be at least 32 characters long for security. " +
                "Current length: " + secret.length() + ". " +
                "Please set JWT_SECRET environment variable with a longer secret."
            );
        }
        
        try {
            return Keys.hmacShaKeyFor(secret.getBytes());
        } catch (Exception e) {
            throw new IllegalStateException(
                "Failed to create JWT signing key: " + e.getMessage() + 
                ". Please check your JWT_SECRET configuration.", e
            );
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            throw new RuntimeException("JWT token has expired", e);
        } catch (io.jsonwebtoken.security.SignatureException e) {
            throw new RuntimeException("Invalid JWT signature. Token may have been tampered with or secret changed.", e);
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            throw new RuntimeException("Invalid JWT token format", e);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JWT token: " + e.getMessage(), e);
        }
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public String generateToken(String username) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, username);
    }

    private String createToken(Map<String, Object> claims, String subject) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS256)
                .compact();
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }

    public String generateToken(UserDetails userDetails, Long userId) {
        try {
            Map<String, Object> claims = new HashMap<>();
            claims.put("userId", userId);
            String token = createToken(claims, userDetails.getUsername());
            logger.debug("Generated JWT token for user: {} with userId: {}", userDetails.getUsername(), userId);
            return token;
        } catch (Exception e) {
            logger.error("Failed to generate JWT token for user: {} - {}", userDetails.getUsername(), e.getMessage(), e);
            throw new RuntimeException("Failed to generate JWT token: " + e.getMessage(), e);
        }
    }
}