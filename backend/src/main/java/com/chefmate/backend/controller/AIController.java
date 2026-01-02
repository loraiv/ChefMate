package com.chefmate.backend.controller;

import com.chefmate.backend.service.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final JwtService jwtService;

    public AIController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @PostMapping("/chat")
    public ResponseEntity<Map<String, String>> chatWithAI(
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Map<String, String> response = new HashMap<>();
        response.put("response", "Здравей! Аз съм твоят готварски асистент. За съжаление AI функционалността все още не е напълно имплементирана. " +
                "Твоето съобщение беше: \"" + message + "\". " +
                "Скоро ще мога да ти помагам с рецепти, съвети за готвене и много повече!");

        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        try {
            return jwtService.extractUserId(token);
        } catch (Exception e) {
            return null;
        }
    }
}

