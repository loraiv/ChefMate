package com.chefmate.backend.controller;

import com.chefmate.backend.service.DatabaseCleanupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class DatabaseCleanupController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupController.class);

    private final DatabaseCleanupService databaseCleanupService;

    public DatabaseCleanupController(DatabaseCleanupService databaseCleanupService) {
        this.databaseCleanupService = databaseCleanupService;
    }

    /**
     * Cleans the entire database - all users, recipes, comments, likes, etc.
     * WARNING: This operation is irreversible!
     * 
     * @return Cleanup result with information about deleted records
     */
    @PostMapping("/cleanup-database")
    public ResponseEntity<Map<String, Object>> cleanupDatabase() {
        logger.warn("Database cleanup requested via API");
        
        DatabaseCleanupService.CleanupResult result = databaseCleanupService.cleanupAllData();
        
        Map<String, Object> response = new HashMap<>();
        response.put("success", result.isSuccess());
        response.put("message", result.getMessage());
        response.put("details", Map.of(
            "commentLikesDeleted", result.getCommentLikesDeleted(),
            "commentsDeleted", result.getCommentsDeleted(),
            "recipeLikesDeleted", result.getRecipeLikesDeleted(),
            "recipeImagesDeleted", result.getRecipeImagesDeleted(),
            "shoppingListItemsDeleted", result.getShoppingListItemsDeleted(),
            "shoppingListsDeleted", result.getShoppingListsDeleted(),
            "passwordResetTokensDeleted", result.getPasswordResetTokensDeleted(),
            "recipesDeleted", result.getRecipesDeleted(),
            "usersDeleted", result.getUsersDeleted(),
            "totalDeleted", result.getTotalDeleted()
        ));
        
        if (result.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(500).body(response);
        }
    }

    /**
     * Gets information about the current state of the database (without deleting anything)
     */
    @GetMapping("/database-stats")
    public ResponseEntity<Map<String, Object>> getDatabaseStats() {
        Map<String, Object> stats = new HashMap<>();
        try {
            stats.put("users", databaseCleanupService.getUserCount());
            stats.put("recipes", databaseCleanupService.getRecipeCount());
            stats.put("comments", databaseCleanupService.getCommentCount());
            stats.put("recipeLikes", databaseCleanupService.getRecipeLikeCount());
            stats.put("commentLikes", databaseCleanupService.getCommentLikeCount());
            stats.put("shoppingLists", databaseCleanupService.getShoppingListCount());
            stats.put("passwordResetTokens", databaseCleanupService.getPasswordResetTokenCount());
            stats.put("message", "Database statistics retrieved successfully");
        } catch (Exception e) {
            stats.put("error", e.getMessage());
        }
        return ResponseEntity.ok(stats);
    }
}
