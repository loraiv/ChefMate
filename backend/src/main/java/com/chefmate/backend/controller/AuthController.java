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
            
            // Създава AuthResponse обект (като при login)
            org.springframework.security.core.userdetails.UserDetails userDetails = 
                org.springframework.security.core.userdetails.User.builder()
                    .username(user.getUsername())
                    .password(user.getPassword())
                    .authorities("ROLE_USER")
                    .build();
            
            String token = userService.generateTokenForUser(userDetails, user.getId());
            
            AuthResponse response = new AuthResponse();
            response.setToken(token);
            response.setType("Bearer");
            response.setId(user.getId());
            response.setUsername(user.getUsername());
            response.setEmail(user.getEmail());
            response.setFirstName(user.getFirstName());
            response.setLastName(user.getLastName());
            
            return ResponseEntity.ok(response);
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