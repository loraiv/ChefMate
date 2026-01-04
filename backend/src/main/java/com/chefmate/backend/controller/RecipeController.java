package com.chefmate.backend.controller;

import com.chefmate.backend.dto.CommentResponse;
import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.service.FileStorageService;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.RecipeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/recipes")
@CrossOrigin(origins = "*")
public class RecipeController {

    private static final Logger logger = LoggerFactory.getLogger(RecipeController.class);

    private final RecipeService recipeService;
    private final JwtService jwtService;
    private final FileStorageService fileStorageService;
    private final ObjectMapper objectMapper;

    public RecipeController(RecipeService recipeService, JwtService jwtService, 
                           FileStorageService fileStorageService, ObjectMapper objectMapper) {
        this.recipeService = recipeService;
        this.jwtService = jwtService;
        this.fileStorageService = fileStorageService;
        this.objectMapper = objectMapper;
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

    @PostMapping(consumes = {"multipart/form-data"})
    public ResponseEntity<?> createRecipe(
            @RequestPart("recipe") String recipeJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        logger.info("Received recipe creation request. Token present: {}", token != null);
        logger.info("Recipe JSON length: {}", recipeJson != null ? recipeJson.length() : 0);
        logger.info("Recipe JSON content: {}", recipeJson);
        logger.debug("Images present: {}", images != null && !images.isEmpty());
        
        if (recipeJson == null || recipeJson.trim().isEmpty()) {
            logger.error("Recipe JSON is null or empty");
            return ResponseEntity.badRequest().body("Recipe JSON is required");
        }
        
        Long userId = getUserIdFromToken(token);
        if (userId == null) {
            logger.warn("Unauthorized request - invalid or missing token");
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }

        logger.info("User ID extracted: {}", userId);

        try {
            logger.info("Attempting to parse JSON: {}", recipeJson);
            RecipeRequest request = objectMapper.readValue(recipeJson, RecipeRequest.class);
            logger.info("Recipe request parsed successfully. Title: {}", request.getTitle());
            
            // Save images if provided
            List<String> imageUrls = new ArrayList<>();
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        logger.info("Saving image file: {}", image.getOriginalFilename());
                        String fileName = fileStorageService.storeFile(image);
                        String imageUrl = "/uploads/" + fileName;
                        imageUrls.add(imageUrl);
                        logger.info("Image saved with URL: {}", imageUrl);
                    }
                }
            }
            
            RecipeResponse response = recipeService.createRecipeWithImages(request, userId, imageUrls);
            logger.info("Recipe created successfully with ID: {}", response.getId());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error creating recipe", e);
            return ResponseEntity.badRequest().body("Error creating recipe: " + e.getMessage());
        }
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

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRecipes(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        logger.info("Received request for user recipes. UserId: {}, Token present: {}", userId, token != null);
        
        try {
            Long currentUserId = getUserIdFromToken(token);
            logger.info("Current user ID from token: {}", currentUserId);
            
            List<RecipeResponse> recipes = recipeService.getUserRecipes(userId, currentUserId);
            logger.info("Found {} recipes for user {}", recipes.size(), userId);
            
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            logger.error("Error fetching user recipes for userId: {}", userId, e);
            return ResponseEntity.status(500).body("Error fetching user recipes: " + e.getMessage());
        }
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
