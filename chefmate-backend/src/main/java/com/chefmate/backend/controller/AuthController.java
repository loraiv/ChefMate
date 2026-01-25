package com.chefmate.backend.controller;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.ChangePasswordRequest;
import com.chefmate.backend.dto.ChangeUsernameRequest;
import com.chefmate.backend.dto.ForgotPasswordRequest;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.dto.ResetPasswordRequest;
import com.chefmate.backend.dto.UserProfileResponse;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.UserRepository;
import com.chefmate.backend.service.AuthService;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.UserService;
import com.chefmate.backend.utils.JwtUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtService jwtService;
    private final UserService userService;
    private final UserRepository userRepository;

    public AuthController(AuthService authService, JwtService jwtService, UserService userService, UserRepository userRepository) {
        this.authService = authService;
        this.jwtService = jwtService;
        this.userService = userService;
        this.userRepository = userRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        try {
            AuthResponse response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (org.springframework.security.core.AuthenticationException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Invalid email or password. Please check your credentials and try again.");
            return ResponseEntity.status(401).body(error);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to sign in. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    @DeleteMapping("/delete-account")
    public ResponseEntity<Map<String, String>> deleteAccount(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please sign in to continue");
                return ResponseEntity.status(401).body(error);
            }

            userService.deleteAccount(userId);
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Your account has been deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to delete account. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Get current user profile information
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please sign in to continue");
                return ResponseEntity.status(401).body(error);
            }

            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            UserProfileResponse response = new UserProfileResponse();
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            response.setRole(user.getRole().name());
            response.setProfileImageUrl(user.getProfileImageUrl());

            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to retrieve user profile. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Change username for current user
     */
    @PutMapping("/change-username")
    public ResponseEntity<?> changeUsername(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody ChangeUsernameRequest request) {
        try {
            Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please sign in to continue");
                return ResponseEntity.status(401).body(error);
            }

            if (request.getNewUsername() == null || request.getNewUsername().trim().isEmpty()) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Username cannot be empty");
                return ResponseEntity.status(400).body(error);
            }

            userService.changeUsername(userId, request.getNewUsername());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Username updated successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to change username. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Change password for current user
     */
    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody ChangePasswordRequest request) {
        try {
            Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
            if (userId == null) {
                Map<String, String> error = new HashMap<>();
                error.put("error", "Please sign in to continue");
                return ResponseEntity.status(401).body(error);
            }

            java.util.Map<String, String> response = authService.changePassword(userId, request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to change password. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@RequestBody ForgotPasswordRequest request) {
        try {
            java.util.Map<String, String> response = authService.forgotPassword(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to process password reset request. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody ResetPasswordRequest request) {
        try {
            java.util.Map<String, String> response = authService.resetPassword(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(400).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", "Unable to reset password. Please try again later.");
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Web endpoint for password reset link that redirects to the Android app deep link
     * This allows the link to work in browsers and redirect to the app
     */
    @GetMapping("/reset-password")
    public ResponseEntity<String> resetPasswordRedirect(@RequestParam("token") String token) {
        // Create deep link for Android app
        String deepLink = "chefmate://reset-password?token=" + token;
        
        // HTML page that automatically redirects to the app
        String html = "<!DOCTYPE html>" +
            "<html>" +
            "<head>" +
            "<meta charset='UTF-8'>" +
            "<meta name='viewport' content='width=device-width, initial-scale=1.0'>" +
            "<title>ChefMate - Password Reset</title>" +
            "<style>" +
            "body { font-family: Arial, sans-serif; text-align: center; padding: 50px; background: #f5f5f5; }" +
            ".container { max-width: 500px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; box-shadow: 0 2px 10px rgba(0,0,0,0.1); }" +
            "h1 { color: #7C3AED; }" +
            "a { display: inline-block; margin-top: 20px; padding: 12px 30px; background-color: #7C3AED; color: white; text-decoration: none; border-radius: 5px; }" +
            "a:hover { background-color: #6D28D9; }" +
            "</style>" +
            "<script>" +
            "// Try to open the app immediately" +
            "window.location.href = '" + deepLink + "';" +
            "// Fallback: show link if app doesn't open" +
            "setTimeout(function() {" +
            "  document.getElementById('fallback').style.display = 'block';" +
            "}, 1000);" +
            "</script>" +
            "</head>" +
            "<body>" +
            "<div class='container'>" +
            "<h1>Password Reset</h1>" +
            "<p>Redirecting to the app...</p>" +
            "<div id='fallback' style='display: none;'>" +
            "<p>If the app does not open automatically, click the link below:</p>" +
            "<a href='" + deepLink + "'>Open ChefMate</a>" +
            "</div>" +
            "</div>" +
            "</body>" +
            "</html>";
        
        return ResponseEntity.status(HttpStatus.OK)
            .header("Content-Type", "text/html; charset=UTF-8")
            .body(html);
    }
}