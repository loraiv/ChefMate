package com.chefmate.backend.controller;

import com.chefmate.backend.dto.CommentResponse;
import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.service.FileStorageService;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.RecipeService;
import com.chefmate.backend.utils.JwtUtils;
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
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
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
        
        Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
        List<RecipeResponse> recipes = recipeService.getAllRecipes(currentUserId);
        return ResponseEntity.ok(recipes);
    }

    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
        RecipeResponse recipe = recipeService.getRecipeById(id, currentUserId);
        return ResponseEntity.ok(recipe);
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getUserRecipes(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        logger.info("Received request for user recipes. UserId: {}, Token present: {}", userId, token != null);
        
        try {
            Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
            logger.info("Current user ID from token: {}", currentUserId);
            
            List<RecipeResponse> recipes = recipeService.getUserRecipes(userId, currentUserId);
            logger.info("Found {} recipes for user {}", recipes.size(), userId);
            
            return ResponseEntity.ok(recipes);
        } catch (Exception e) {
            logger.error("Error fetching user recipes for userId: {}", userId, e);
            return ResponseEntity.status(500).body("Error fetching user recipes: " + e.getMessage());
        }
    }

    @PutMapping(value = "/{id}", consumes = {"multipart/form-data"})
    public ResponseEntity<?> updateRecipe(
            @PathVariable Long id,
            @RequestPart("recipe") String recipeJson,
            @RequestPart(value = "images", required = false) List<MultipartFile> images,
            @RequestPart(value = "existingImageUrls", required = false) String existingImageUrlsJson,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        logger.info("Received recipe update request for ID: {}. Token present: {}", id, token != null);
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            logger.warn("Unauthorized request - invalid or missing token");
            return ResponseEntity.status(401).body("Unauthorized: Invalid or missing token");
        }

        try {
            RecipeRequest request = objectMapper.readValue(recipeJson, RecipeRequest.class);
            logger.info("Recipe request parsed successfully. Title: {}", request.getTitle());
            
            // Handle images
            List<String> imageUrls = new ArrayList<>();
            
            // Add existing image URLs if provided
            if (existingImageUrlsJson != null && !existingImageUrlsJson.trim().isEmpty()) {
                try {
                    List<String> existingUrls = objectMapper.readValue(existingImageUrlsJson, 
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
                    imageUrls.addAll(existingUrls);
                    logger.info("Added {} existing image URLs", existingUrls.size());
                } catch (Exception e) {
                    logger.warn("Failed to parse existing image URLs: {}", e.getMessage());
                }
            }
            
            // Add new uploaded images
            if (images != null && !images.isEmpty()) {
                for (MultipartFile image : images) {
                    if (image != null && !image.isEmpty()) {
                        logger.info("Saving new image file: {}", image.getOriginalFilename());
                        String fileName = fileStorageService.storeFile(image);
                        String imageUrl = "/uploads/" + fileName;
                        imageUrls.add(imageUrl);
                        logger.info("New image saved with URL: {}", imageUrl);
                    }
                }
            }
            
            RecipeResponse response = recipeService.updateRecipeWithImages(id, request, userId, imageUrls);
            logger.info("Recipe updated successfully with ID: {}", id);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            logger.error("Error updating recipe: {}", e.getMessage(), e);
            return ResponseEntity.status(403).body("Error: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating recipe", e);
            return ResponseEntity.badRequest().body("Error updating recipe: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteRecipe(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
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
        
        Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (currentUserId == null) {
            return ResponseEntity.status(401).build();
        }
        
        List<RecipeResponse> recipes = recipeService.searchRecipes(query, difficulty, maxTime, currentUserId);
        return ResponseEntity.ok(recipes);
    }

    @PostMapping("/{recipeId}/like")
    public ResponseEntity<Void> likeRecipe(
            @PathVariable Long recipeId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
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
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
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
            Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
            List<CommentResponse> comments = recipeService.getRecipeComments(recipeId, currentUserId);
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
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
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

    @PostMapping("/comments/{commentId}/reply")
    public ResponseEntity<CommentResponse> replyToComment(
            @PathVariable Long commentId,
            @RequestBody Map<String, String> request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        String content = request.get("content");
        if (content == null || content.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            CommentResponse response = recipeService.replyToComment(commentId, content, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).body(null);
        }
    }

    @PostMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> likeComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            recipeService.likeComment(commentId, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @DeleteMapping("/comments/{commentId}/like")
    public ResponseEntity<Void> unlikeComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            recipeService.unlikeComment(commentId, userId);
            return ResponseEntity.ok().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }

        try {
            recipeService.deleteComment(commentId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @GetMapping("/test")
    public String test() {
        return "Recipe controller is working!";
    }
}
