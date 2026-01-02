package com.chefmate.backend.controller;

import com.chefmate.backend.dto.CommentResponse;
import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;
    private final JwtService jwtService;

    public RecipeController(RecipeService recipeService, JwtService jwtService) {
        this.recipeService = recipeService;
        this.jwtService = jwtService;
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

    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(
            @RequestBody RecipeRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        RecipeResponse response = recipeService.createRecipe(request, userId);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAllRecipes(
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long currentUserId = getUserIdFromToken(token);
        List<RecipeResponse> recipes = recipeService.getAllRecipes(currentUserId);
        return ResponseEntity.ok(recipes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long currentUserId = getUserIdFromToken(token);
        RecipeResponse recipe = recipeService.getRecipeById(id, currentUserId);
        return ResponseEntity.ok(recipe);
    }

    @PutMapping("/{id}")
    public ResponseEntity<RecipeResponse> updateRecipe(
            @PathVariable Long id,
            @RequestBody RecipeRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            RecipeResponse response = recipeService.updateRecipe(id, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            recipeService.deleteRecipe(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/search")
    public ResponseEntity<List<RecipeResponse>> searchRecipes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) Integer maxTime,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long currentUserId = getUserIdFromToken(token);
        List<RecipeResponse> recipes = recipeService.searchRecipes(query, difficulty, maxTime, currentUserId);
        return ResponseEntity.ok(recipes);
    }

    @PostMapping("/{recipeId}/like")
    public ResponseEntity<Void> likeRecipe(
            @PathVariable Long recipeId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            recipeService.likeRecipe(recipeId, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @DeleteMapping("/{recipeId}/like")
    public ResponseEntity<Void> unlikeRecipe(
            @PathVariable Long recipeId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            recipeService.unlikeRecipe(recipeId, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/{recipeId}/comments")
    public ResponseEntity<List<CommentResponse>> getRecipeComments(
            @PathVariable Long recipeId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        try {
            List<CommentResponse> comments = recipeService.getRecipeComments(recipeId);
            return ResponseEntity.ok(comments);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @PostMapping("/{recipeId}/comments")
    public ResponseEntity<CommentResponse> addComment(
            @PathVariable Long recipeId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            CommentResponse response = recipeService.addComment(recipeId, content, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Recipe controller is working!";
    }
}
