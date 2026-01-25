package com.chefmate.backend.utils;

import com.chefmate.backend.service.JwtService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class for JWT token operations
 */
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    /**
     * Extracts user ID from JWT token
     * 
     * @param token JWT token (with or without "Bearer " prefix)
     * @param jwtService JWT service instance
     * @return User ID if token is valid, null otherwise
     */
    public static Long getUserIdFromToken(String token, JwtService jwtService) {
        if (token == null || token.trim().isEmpty()) {
            return null;
        }
        
        // Remove "Bearer " prefix if present
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        
        try {
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            logger.debug("Failed to extract user ID from token: {}", e.getMessage());
            return null;
        }
    }
}
