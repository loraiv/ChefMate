package com.chefmate.backend.service;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.ChangePasswordRequest;
import com.chefmate.backend.dto.ForgotPasswordRequest;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.dto.ResetPasswordRequest;
import com.chefmate.backend.entity.PasswordResetToken;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.PasswordResetTokenRepository;
import com.chefmate.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;

@Service
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    public AuthService(
            AuthenticationManager authenticationManager,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            PasswordResetTokenRepository passwordResetTokenRepository,
            EmailService emailService
    ) {
        this.authenticationManager = authenticationManager;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByUsernameIgnoreCase(request.getUsername())) {
            throw new RuntimeException("This username is already taken. Please choose a different one.");
        }
        if (userRepository.existsByEmailIgnoreCase(request.getEmail())) {
            throw new RuntimeException("This email is already registered. Please use a different email address or sign in.");
        }

        User user = new User();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        User savedUser = userRepository.save(user);

        // Build Spring Security user details for JWT
        String role = "ROLE_" + savedUser.getRole().name();
        org.springframework.security.core.userdetails.User userDetails =
                new org.springframework.security.core.userdetails.User(
                        savedUser.getUsername(),
                        savedUser.getPassword(),
                        java.util.List.of(() -> role)
                );

        String token = jwtService.generateToken(userDetails, savedUser.getId());

        AuthResponse authResponse = new AuthResponse();
        authResponse.setToken(token);
        authResponse.setId(savedUser.getId());
        authResponse.setUsername(savedUser.getUsername());
        authResponse.setEmail(savedUser.getEmail());
        authResponse.setFirstName(savedUser.getFirstName());
        authResponse.setLastName(savedUser.getLastName());
        authResponse.setRole(savedUser.getRole().name());

        return authResponse;
    }

    public AuthResponse login(LoginRequest request) {
        try {
            logger.debug("Attempting login for: {}", request.getUsernameOrEmail());
            
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(
                            request.getUsernameOrEmail(),
                            request.getPassword()
                    )
            );

            UserDetails userDetails = (UserDetails) authentication.getPrincipal();
            logger.debug("Authentication successful for: {}", userDetails.getUsername());
            
            // Find full User entity (by username or email)
            User user = userRepository.findByUsernameIgnoreCase(userDetails.getUsername())
                    .orElseGet(() -> userRepository.findByEmailIgnoreCase(userDetails.getUsername())
                            .orElseThrow(() -> {
                                logger.error("User not found after authentication: {}", userDetails.getUsername());
                                return new RuntimeException("User not found");
                            }));

            logger.debug("User found: {} with role: {}", user.getUsername(), user.getRole());

            // Build Spring Security user details with correct role
            String role = "ROLE_" + user.getRole().name();
            org.springframework.security.core.userdetails.User userDetailsWithRole =
                    new org.springframework.security.core.userdetails.User(
                            user.getUsername(),
                            user.getPassword(),
                            java.util.List.of(() -> role)
                    );

            String token = jwtService.generateToken(userDetailsWithRole, user.getId());

            AuthResponse authResponse = new AuthResponse();
            authResponse.setToken(token);
            authResponse.setId(user.getId());
            authResponse.setUsername(user.getUsername());
            authResponse.setEmail(user.getEmail());
            authResponse.setFirstName(user.getFirstName());
            authResponse.setLastName(user.getLastName());
            authResponse.setRole(user.getRole().name());

            logger.info("Login successful for user: {} with role: {}", user.getUsername(), user.getRole());
            return authResponse;
        } catch (org.springframework.security.core.AuthenticationException e) {
            logger.warn("Authentication failed for: {} - {}", request.getUsernameOrEmail(), e.getMessage());
            throw new BadCredentialsException("Invalid username/email or password");
        } catch (Exception e) {
            logger.error("Login error for: {} - {}", request.getUsernameOrEmail(), e.getMessage(), e);
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    @Transactional
    public java.util.Map<String, String> forgotPassword(ForgotPasswordRequest request) {
        logger.info("Processing forgot password request for email: {}", request.getEmail());
        
        User user = userRepository.findByEmailIgnoreCase(request.getEmail())
                .orElse(null);
        
        // Always return success to prevent email enumeration
        java.util.Map<String, String> response = new java.util.HashMap<>();
        
        if (user == null) {
            logger.warn("Forgot password requested for non-existent email: {}", request.getEmail());
            response.put("message", "If the email exists, a password reset link has been sent to your email address.");
            return response;
        }
        
        // Generate reset token
        String token = java.util.UUID.randomUUID().toString();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(1);
        
        // Delete any existing tokens for this user
        passwordResetTokenRepository.deleteByUserId(user.getId());
        
        // Create new token
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);
        
        logger.info("Generated password reset token for user: {}", user.getEmail());
        
        // Try to send email
        try {
            logger.info("Calling emailService.sendPasswordResetEmail for: {}", user.getEmail());
            boolean emailSent = emailService.sendPasswordResetEmail(user.getEmail(), token);
            logger.info("EmailService returned: {}", emailSent);
            if (emailSent) {
                response.put("message", "Password reset instructions have been sent to your email address. Please check your email and click the link to reset your password.");
                logger.info("Successfully sent password reset email to: {}", user.getEmail());
            } else {
                logger.warn("EmailService returned false, but no exception was thrown");
                response.put("token", token);
                response.put("message", "Failed to send email. Please use this token to reset your password: " + token);
            }
        } catch (RuntimeException e) {
            // If email service is not configured or failed, return the token directly
            logger.error("Email sending failed with exception: {}", e.getMessage(), e);
            response.put("token", token);
            if (e.getMessage() != null && e.getMessage().contains("not configured")) {
                response.put("message", "Email service is not configured. Please use this token to reset your password.");
            } else {
                response.put("message", "Failed to send email: " + e.getMessage() + ". Please use this token to reset your password: " + token);
            }
        } catch (Exception e) {
            logger.error("Unexpected exception while sending email: {}", e.getMessage(), e);
            response.put("token", token);
            response.put("message", "Unexpected error: " + e.getMessage() + ". Please use this token to reset your password: " + token);
        }
        
        return response;
    }

    @Transactional
    public java.util.Map<String, String> resetPassword(ResetPasswordRequest request) {
        logger.info("Processing password reset request");
        
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new RuntimeException("Reset token is required. Please use the link from your email.");
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("Please enter a new password");
        }
        
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("Password must be at least 6 characters long");
        }
        
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.getToken())
                .orElseThrow(() -> new RuntimeException("Invalid or expired reset token"));
        
        if (resetToken.getUsed()) {
            throw new RuntimeException("This reset link has already been used. Please request a new password reset.");
        }
        
        if (resetToken.isExpired()) {
            throw new RuntimeException("This reset link has expired. Please request a new password reset.");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        // Mark token as used
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        logger.info("Password reset successful for user: {}", user.getEmail());
        
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Your password has been reset successfully. You can now sign in with your new password.");
        return response;
    }

    @Transactional
    public java.util.Map<String, String> changePassword(Long userId, ChangePasswordRequest request) {
        logger.info("Processing password change request for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (request.getCurrentPassword() == null || request.getCurrentPassword().trim().isEmpty()) {
            throw new RuntimeException("Please enter your current password");
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().trim().isEmpty()) {
            throw new RuntimeException("Please enter a new password");
        }
        
        if (request.getNewPassword().length() < 6) {
            throw new RuntimeException("New password must be at least 6 characters long");
        }
        
        // Verify current password
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            logger.warn("Password change failed for user {}: current password is incorrect", user.getEmail());
            throw new RuntimeException("Current password is incorrect. Please try again.");
        }
        
        // Check if new password is different from current
        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new RuntimeException("New password must be different from your current password");
        }
        
        // Update password
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", user.getEmail());
        
        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Your password has been changed successfully");
        return response;
    }
}

