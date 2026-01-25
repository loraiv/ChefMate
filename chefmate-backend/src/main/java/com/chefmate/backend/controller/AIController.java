package com.chefmate.backend.controller;

import com.chefmate.backend.dto.AiResponse;
import com.chefmate.backend.service.AiService;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.utils.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ai")
public class AIController {

    private final JwtService jwtService;
    private final AiService aiService;

    public AIController(JwtService jwtService, AiService aiService) {
        this.jwtService = jwtService;
        this.aiService = aiService;
    }

    @PostMapping("/chat")
    public ResponseEntity<AiResponse> chatWithAI(
            @RequestBody Map<String, Object> request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String message = (String) request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        Long recipeId = null;
        if (request.get("recipeId") != null) {
            if (request.get("recipeId") instanceof Number) {
                recipeId = ((Number) request.get("recipeId")).longValue();
            }
        }

        Map<String, Object> cookingContext = null;
        if (request.get("cookingContext") != null) {
            cookingContext = (Map<String, Object>) request.get("cookingContext");
        }

        String aiResponseText = aiService.getAiResponse(message, recipeId, cookingContext);
        AiResponse response = new AiResponse(aiResponseText);

        return ResponseEntity.ok(response);
    }
}
