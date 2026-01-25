package com.chefmate.backend.service;

import com.chefmate.backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DatabaseCleanupService {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseCleanupService.class);

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final RecipeLikeRepository recipeLikeRepository;
    private final RecipeImageRepository recipeImageRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public DatabaseCleanupService(
            CommentLikeRepository commentLikeRepository,
            CommentRepository commentRepository,
            RecipeLikeRepository recipeLikeRepository,
            RecipeImageRepository recipeImageRepository,
            ShoppingListItemRepository shoppingListItemRepository,
            ShoppingListRepository shoppingListRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RecipeRepository recipeRepository,
            UserRepository userRepository) {
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.recipeLikeRepository = recipeLikeRepository;
        this.recipeImageRepository = recipeImageRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public CleanupResult cleanupAllData() {
        logger.info("Starting database cleanup...");
        CleanupResult result = new CleanupResult();

        try {
            // Method 1: Try using native SQL to disable constraints temporarily (PostgreSQL)
            try {
                cleanupWithNativeSQL(result);
                result.setSuccess(true);
                result.setMessage("Database cleanup completed successfully using native SQL");
                logger.info("Database cleanup completed successfully. Total records deleted: {}", result.getTotalDeleted());
                return result;
            } catch (Exception nativeException) {
                logger.warn("Native SQL cleanup failed, trying repository method: {}", nativeException.getMessage());
            }

            // Method 2: Fallback to repository deleteAll (respects foreign keys)
            cleanupWithRepositories(result);
            result.setSuccess(true);
            result.setMessage("Database cleanup completed successfully");
            logger.info("Database cleanup completed successfully. Total records deleted: {}", result.getTotalDeleted());

        } catch (Exception e) {
            logger.error("Error during database cleanup", e);
            result.setSuccess(false);
            result.setMessage("Error during cleanup: " + e.getMessage());
        }

        return result;
    }

    @Transactional
    private void cleanupWithNativeSQL(CleanupResult result) {
        // Disable foreign key constraints temporarily (PostgreSQL specific)
        entityManager.createNativeQuery("SET session_replication_role = 'replica'").executeUpdate();
        
        try {
            // Delete in order (respecting dependencies)
            result.setCommentLikesDeleted(executeDelete("comment_likes"));
            result.setCommentsDeleted(executeDelete("comments"));
            result.setRecipeLikesDeleted(executeDelete("recipe_likes"));
            result.setRecipeImagesDeleted(executeDelete("recipe_images"));
            result.setShoppingListItemsDeleted(executeDelete("shopping_list_items"));
            result.setShoppingListsDeleted(executeDelete("shopping_lists"));
            result.setPasswordResetTokensDeleted(executeDelete("password_reset_tokens"));
            
            // Delete collection tables
            result.setRecipeImagesDeleted(result.getRecipeImagesDeleted() + executeDelete("recipe_image_urls"));
            executeDelete("recipe_ingredients");
            executeDelete("recipe_steps");
            
            result.setRecipesDeleted(executeDelete("recipes"));
            result.setUsersDeleted(executeDelete("users"));
        } finally {
            // Re-enable foreign key constraints
            entityManager.createNativeQuery("SET session_replication_role = 'origin'").executeUpdate();
        }
    }

    @Transactional
    private void cleanupWithRepositories(CleanupResult result) {
        // 1. Delete CommentLikes (depends on Comment and User)
        long commentLikesCount = commentLikeRepository.count();
        commentLikeRepository.deleteAll();
        result.setCommentLikesDeleted(commentLikesCount);
        logger.info("Deleted {} comment likes", commentLikesCount);

        // 2. Delete Comments (depends on Recipe and User)
        long commentsCount = commentRepository.count();
        commentRepository.deleteAll();
        result.setCommentsDeleted(commentsCount);
        logger.info("Deleted {} comments", commentsCount);

        // 3. Delete RecipeLikes (depends on Recipe and User)
        long recipeLikesCount = recipeLikeRepository.count();
        recipeLikeRepository.deleteAll();
        result.setRecipeLikesDeleted(recipeLikesCount);
        logger.info("Deleted {} recipe likes", recipeLikesCount);

        // 4. Delete RecipeImages (depends on Recipe)
        long recipeImagesCount = recipeImageRepository.count();
        recipeImageRepository.deleteAll();
        result.setRecipeImagesDeleted(recipeImagesCount);
        logger.info("Deleted {} recipe images", recipeImagesCount);

        // 5. Delete ShoppingListItems (depends on ShoppingList)
        long shoppingListItemsCount = shoppingListItemRepository.count();
        shoppingListItemRepository.deleteAll();
        result.setShoppingListItemsDeleted(shoppingListItemsCount);
        logger.info("Deleted {} shopping list items", shoppingListItemsCount);

        // 6. Delete ShoppingLists (depends on User)
        long shoppingListsCount = shoppingListRepository.count();
        shoppingListRepository.deleteAll();
        result.setShoppingListsDeleted(shoppingListsCount);
        logger.info("Deleted {} shopping lists", shoppingListsCount);

        // 7. Delete PasswordResetTokens (depends on User)
        long passwordResetTokensCount = passwordResetTokenRepository.count();
        passwordResetTokenRepository.deleteAll();
        result.setPasswordResetTokensDeleted(passwordResetTokensCount);
        logger.info("Deleted {} password reset tokens", passwordResetTokensCount);

        // 8. Delete Recipes (depends on User)
        long recipesCount = recipeRepository.count();
        recipeRepository.deleteAll();
        result.setRecipesDeleted(recipesCount);
        logger.info("Deleted {} recipes", recipesCount);

        // 9. Delete collection tables explicitly (in case they're not cascade deleted)
        deleteCollectionTables();

        // 10. Delete Users (base table)
        long usersCount = userRepository.count();
        userRepository.deleteAll();
        result.setUsersDeleted(usersCount);
        logger.info("Deleted {} users", usersCount);
    }

    private long executeDelete(String tableName) {
        try {
            int deleted = entityManager.createNativeQuery("DELETE FROM " + tableName).executeUpdate();
            logger.info("Deleted {} records from {}", deleted, tableName);
            return deleted;
        } catch (Exception e) {
            logger.warn("Could not delete from {}: {}", tableName, e.getMessage());
            return 0;
        }
    }

    @Transactional
    private void deleteCollectionTables() {
        try {
            // Delete from collection tables (these are created by @ElementCollection)
            entityManager.createNativeQuery("DELETE FROM recipe_image_urls").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM recipe_ingredients").executeUpdate();
            entityManager.createNativeQuery("DELETE FROM recipe_steps").executeUpdate();
            logger.info("Deleted data from collection tables (recipe_image_urls, recipe_ingredients, recipe_steps)");
        } catch (Exception e) {
            logger.warn("Could not delete collection tables (they may not exist or may be empty): {}", e.getMessage());
        }
    }

    // Statistics methods
    public long getUserCount() {
        return userRepository.count();
    }

    public long getRecipeCount() {
        return recipeRepository.count();
    }

    public long getCommentCount() {
        return commentRepository.count();
    }

    public long getRecipeLikeCount() {
        return recipeLikeRepository.count();
    }

    public long getCommentLikeCount() {
        return commentLikeRepository.count();
    }

    public long getShoppingListCount() {
        return shoppingListRepository.count();
    }

    public long getPasswordResetTokenCount() {
        return passwordResetTokenRepository.count();
    }

    public static class CleanupResult {
        private boolean success;
        private String message;
        private long commentLikesDeleted;
        private long commentsDeleted;
        private long recipeLikesDeleted;
        private long recipeImagesDeleted;
        private long shoppingListItemsDeleted;
        private long shoppingListsDeleted;
        private long passwordResetTokensDeleted;
        private long recipesDeleted;
        private long usersDeleted;

        public long getTotalDeleted() {
            return commentLikesDeleted + commentsDeleted + recipeLikesDeleted + 
                   recipeImagesDeleted + shoppingListItemsDeleted + shoppingListsDeleted + 
                   passwordResetTokensDeleted + recipesDeleted + usersDeleted;
        }

        // Getters and Setters
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }

        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }

        public long getCommentLikesDeleted() { return commentLikesDeleted; }
        public void setCommentLikesDeleted(long commentLikesDeleted) { this.commentLikesDeleted = commentLikesDeleted; }

        public long getCommentsDeleted() { return commentsDeleted; }
        public void setCommentsDeleted(long commentsDeleted) { this.commentsDeleted = commentsDeleted; }

        public long getRecipeLikesDeleted() { return recipeLikesDeleted; }
        public void setRecipeLikesDeleted(long recipeLikesDeleted) { this.recipeLikesDeleted = recipeLikesDeleted; }

        public long getRecipeImagesDeleted() { return recipeImagesDeleted; }
        public void setRecipeImagesDeleted(long recipeImagesDeleted) { this.recipeImagesDeleted = recipeImagesDeleted; }

        public long getShoppingListItemsDeleted() { return shoppingListItemsDeleted; }
        public void setShoppingListItemsDeleted(long shoppingListItemsDeleted) { this.shoppingListItemsDeleted = shoppingListItemsDeleted; }

        public long getShoppingListsDeleted() { return shoppingListsDeleted; }
        public void setShoppingListsDeleted(long shoppingListsDeleted) { this.shoppingListsDeleted = shoppingListsDeleted; }

        public long getPasswordResetTokensDeleted() { return passwordResetTokensDeleted; }
        public void setPasswordResetTokensDeleted(long passwordResetTokensDeleted) { this.passwordResetTokensDeleted = passwordResetTokensDeleted; }

        public long getRecipesDeleted() { return recipesDeleted; }
        public void setRecipesDeleted(long recipesDeleted) { this.recipesDeleted = recipesDeleted; }

        public long getUsersDeleted() { return usersDeleted; }
        public void setUsersDeleted(long usersDeleted) { this.usersDeleted = usersDeleted; }
    }
}
