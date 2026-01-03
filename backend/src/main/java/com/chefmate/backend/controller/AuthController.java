package com.chefmate.backend.controller;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.ForgotPasswordRequest;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.dto.ResetPasswordRequest;
import com.chefmate.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/test")
    public String test() {
        return "Auth controller is working!";
    }

    @GetMapping("/check-user")
    public ResponseEntity<?> checkUser(@RequestParam String username, @RequestParam String email) {
        try {
            boolean usernameExists = userService.checkUsernameExists(username);
            boolean emailExists = userService.checkEmailExists(email);
            
            return ResponseEntity.ok(
                "Username '" + username + "' exists: " + usernameExists + "\n" +
                "Email '" + email + "' exists: " + emailExists
            );
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Error: " + e.getMessage());
        }
    }

    @DeleteMapping("/clear-users")
    public ResponseEntity<?> clearAllUsers() {
        try {
            int deletedCount = userService.deleteAllUsers();
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Успешно изтрити " + deletedCount + " потребителя");
            response.put("deletedCount", deletedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        }
    }

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            var user = userService.registerUser(request);
            
            // След успешна регистрация, автоматично влез като този потребител и върни AuthResponse
            LoginRequest loginRequest = new LoginRequest();
            loginRequest.setUsernameOrEmail(user.getUsername());
            loginRequest.setPassword(request.getPassword());
            
            AuthResponse authResponse = userService.loginUser(loginRequest);
            return ResponseEntity.ok(authResponse);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        try {
            AuthResponse response = userService.loginUser(request);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        }
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<?> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        try {
            String message = userService.processForgotPassword(request.getEmail());
            Map<String, String> response = new HashMap<>();
            response.put("message", message);
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        }
    }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        try {
            userService.resetPassword(request.getToken(), request.getNewPassword());
            Map<String, String> response = new HashMap<>();
            response.put("message", "Паролата е успешно променена!");
            return ResponseEntity.ok(response);
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Грешка: " + e.getMessage());
        }
    }
}