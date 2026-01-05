package com.chefmate.backend.controller;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request) {
        try {
            var user = userService.registerUser(request);
            return ResponseEntity.ok(
                    "Успешна регистрация!\n" +
                            "ID: " + user.getId() + "\n" +
                            "Потребител: " + user.getUsername() + "\n" +
                            "Имейл: " + user.getEmail()
            );
        } catch (RuntimeException e) {
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
}