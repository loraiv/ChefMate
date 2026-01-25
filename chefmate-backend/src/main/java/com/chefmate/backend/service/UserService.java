package com.chefmate.backend.service;

import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);

    private final UserRepository userRepository;
    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;
    private final RecipeLikeRepository recipeLikeRepository;
    private final RecipeImageRepository recipeImageRepository;
    private final ShoppingListItemRepository shoppingListItemRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final RecipeRepository recipeRepository;

    @PersistenceContext
    private EntityManager entityManager;

    public UserService(
            UserRepository userRepository,
            CommentLikeRepository commentLikeRepository,
            CommentRepository commentRepository,
            RecipeLikeRepository recipeLikeRepository,
            RecipeImageRepository recipeImageRepository,
            ShoppingListItemRepository shoppingListItemRepository,
            ShoppingListRepository shoppingListRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            RecipeRepository recipeRepository
    ) {
        this.userRepository = userRepository;
        this.commentLikeRepository = commentLikeRepository;
        this.commentRepository = commentRepository;
        this.recipeLikeRepository = recipeLikeRepository;
        this.recipeImageRepository = recipeImageRepository;
        this.shoppingListItemRepository = shoppingListItemRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.recipeRepository = recipeRepository;
    }

    @Transactional
    public void deleteAccount(Long userId) {
        logger.info("Deleting account for user ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // 1. Delete CommentLikes on comments made by this user
        List<Long> userCommentIds = commentRepository.findCommentIdsByUserId(userId);
        if (!userCommentIds.isEmpty()) {
            List<com.chefmate.backend.entity.CommentLike> likesOnUserComments = 
                commentLikeRepository.findByCommentIds(userCommentIds);
            commentLikeRepository.deleteAll(likesOnUserComments);
            logger.info("Deleted {} comment likes on user's comments", likesOnUserComments.size());
        }

        // 2. Delete CommentLikes made by this user (using native query for efficiency)
        try {
            int deletedLikes = entityManager.createNativeQuery(
                "DELETE FROM comment_likes WHERE user_id = :userId"
            )
            .setParameter("userId", userId)
            .executeUpdate();
            logger.info("Deleted {} comment likes made by user", deletedLikes);
        } catch (Exception e) {
            logger.warn("Could not delete comment likes by user: {}", e.getMessage());
        }

        // 3. Delete replies to user's comments
        commentRepository.deleteByParentComment_User_Id(userId);

        // 4. Delete all comments by this user
        commentRepository.deleteByUser_Id(userId);
        logger.info("Deleted all comments by user");

        // 5. Delete RecipeLikes by this user
        recipeLikeRepository.deleteByUserId(userId);
        logger.info("Deleted recipe likes by user");

        // 6. Delete RecipeImages for user's recipes
        List<Long> userRecipeIds = recipeRepository.findByUserId(userId).stream()
            .map(recipe -> recipe.getId())
            .toList();
        userRecipeIds.forEach(recipeImageRepository::deleteByRecipeId);
        logger.info("Deleted recipe images for user's recipes");

        // 7. Delete ShoppingListItems for user's shopping lists
        shoppingListRepository.findByUserId(userId).forEach(shoppingList -> {
            shoppingListItemRepository.deleteByShoppingListId(shoppingList.getId());
        });
        logger.info("Deleted shopping list items for user's lists");

        // 8. Delete ShoppingLists by this user
        shoppingListRepository.deleteByUserId(userId);
        logger.info("Deleted shopping lists by user");

        // 9. Delete PasswordResetTokens for this user
        passwordResetTokenRepository.deleteByUserId(userId);
        logger.info("Deleted password reset tokens for user");

        // 10. Delete all recipes by this user (this will also delete collection tables via cascade)
        recipeRepository.deleteByUserId(userId);
        logger.info("Deleted all recipes by user");

        // 11. Finally, delete the user
        userRepository.delete(user);
        logger.info("Successfully deleted user account: {}", userId);
    }

    @Transactional
    public void changeUsername(Long userId, String newUsername) {
        logger.info("Changing username for user ID: {} to: {}", userId, newUsername);
        
        if (newUsername == null || newUsername.trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty. Please enter a username.");
        }
        
        newUsername = newUsername.trim();
        
        // Check if username is already taken
        if (userRepository.existsByUsernameIgnoreCase(newUsername)) {
            throw new RuntimeException("This username is already taken. Please choose a different one.");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));
        
        // Check if it's the same username
        if (user.getUsername().equalsIgnoreCase(newUsername)) {
            throw new RuntimeException("New username must be different from your current username");
        }
        
        user.setUsername(newUsername);
        userRepository.save(user);
        logger.info("Successfully changed username for user ID: {}", userId);
    }
}
