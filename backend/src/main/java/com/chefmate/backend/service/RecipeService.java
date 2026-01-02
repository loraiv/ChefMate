package com.chefmate.backend.service;

import com.chefmate.backend.dto.CommentResponse;
import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.entity.Comment;
import com.chefmate.backend.entity.Recipe;
import com.chefmate.backend.entity.RecipeLike;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.CommentRepository;
import com.chefmate.backend.repository.RecipeLikeRepository;
import com.chefmate.backend.repository.RecipeRepository;
import com.chefmate.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final RecipeLikeRepository recipeLikeRepository;
    private final CommentRepository commentRepository;

    public RecipeService(RecipeRepository recipeRepository,
                         UserRepository userRepository,
                         RecipeLikeRepository recipeLikeRepository,
                         CommentRepository commentRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.recipeLikeRepository = recipeLikeRepository;
        this.commentRepository = commentRepository;
    }

    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        Recipe recipe = new Recipe();
        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());

        Integer totalTime = 0;
        if (request.getPrepTime() != null) {
            totalTime += request.getPrepTime();
        }
        if (request.getCookTime() != null) {
            totalTime += request.getCookTime();
        }
        recipe.setTotalTime(totalTime);

        recipe.setServings(request.getServings());

        if (request.getDifficulty() != null) {
            try {
                Recipe.Difficulty difficultyEnum = Recipe.Difficulty.valueOf(
                        request.getDifficulty().toUpperCase()
                );
                recipe.setDifficulty(difficultyEnum);
            } catch (IllegalArgumentException e) {
                recipe.setDifficulty(Recipe.Difficulty.EASY);
            }
        } else {
            recipe.setDifficulty(Recipe.Difficulty.EASY);
        }
        recipe.setIngredients(request.getIngredients() != null ?
                request.getIngredients() : new ArrayList<>());
        recipe.setSteps(request.getSteps() != null ?
                request.getSteps() : new ArrayList<>());
        recipe.setUser(user);

        Recipe savedRecipe = recipeRepository.save(recipe);

        return convertToResponse(savedRecipe, userId);
    }

    public List<RecipeResponse> getAllRecipes(Long currentUserId) {
        List<Recipe> recipes = recipeRepository.findAll();
        return recipes.stream()
                .map(recipe -> convertToResponse(recipe, currentUserId))
                .collect(Collectors.toList());
    }

    public RecipeResponse getRecipeById(Long id, Long currentUserId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + id));

        Integer currentViews = recipe.getViewsCount() != null ? recipe.getViewsCount() : 0;
        recipe.setViewsCount(currentViews + 1);
        recipeRepository.save(recipe);

        return convertToResponse(recipe, currentUserId);
    }

    public List<RecipeResponse> getUserRecipes(Long userId, Long currentUserId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        return recipes.stream()
                .map(recipe -> convertToResponse(recipe, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public RecipeResponse updateRecipe(Long id, RecipeRequest request, Long userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + id));

        if (!recipe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this recipe");
        }

        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());
        
        Integer totalTime = 0;
        if (request.getPrepTime() != null) totalTime += request.getPrepTime();
        if (request.getCookTime() != null) totalTime += request.getCookTime();
        recipe.setTotalTime(totalTime);
        
        recipe.setServings(request.getServings());
        
        if (request.getDifficulty() != null) {
            try {
                recipe.setDifficulty(Recipe.Difficulty.valueOf(request.getDifficulty().toUpperCase()));
            } catch (IllegalArgumentException e) {
                recipe.setDifficulty(Recipe.Difficulty.EASY);
            }
        }
        
        recipe.setIngredients(request.getIngredients() != null ? request.getIngredients() : new ArrayList<>());
        recipe.setSteps(request.getSteps() != null ? request.getSteps() : new ArrayList<>());
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe updatedRecipe = recipeRepository.save(recipe);
        return convertToResponse(updatedRecipe, userId);
    }

    @Transactional
    public void deleteRecipe(Long id, Long userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + id));

        if (!recipe.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this recipe");
        }

        recipeRepository.delete(recipe);
    }

    public List<RecipeResponse> searchRecipes(String query, String difficulty, Integer maxTime, Long currentUserId) {
        List<Recipe> recipes = recipeRepository.findAll();
        
        return recipes.stream()
                .filter(recipe -> {
                    if (query != null && !query.isEmpty()) {
                        if (!recipe.getTitle().toLowerCase().contains(query.toLowerCase()) &&
                            !recipe.getDescription().toLowerCase().contains(query.toLowerCase())) {
                            return false;
                        }
                    }
                    
                    if (difficulty != null && !difficulty.isEmpty()) {
                        try {
                            Recipe.Difficulty diff = Recipe.Difficulty.valueOf(difficulty.toUpperCase());
                            if (!recipe.getDifficulty().equals(diff)) {
                                return false;
                            }
                        } catch (IllegalArgumentException e) {
                        }
                    }
                    
                    if (maxTime != null && recipe.getTotalTime() != null) {
                        if (recipe.getTotalTime() > maxTime) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .map(recipe -> convertToResponse(recipe, currentUserId))
                .collect(Collectors.toList());
    }

    @Transactional
    public void likeRecipe(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (recipeLikeRepository.existsByRecipeIdAndUserId(recipeId, userId)) {
            return;
        }

        RecipeLike like = new RecipeLike(recipe, user);
        recipeLikeRepository.save(like);

        Integer currentLikes = recipe.getLikesCount() != null ? recipe.getLikesCount() : 0;
        recipe.setLikesCount(currentLikes + 1);
        recipeRepository.save(recipe);
    }

    @Transactional
    public void unlikeRecipe(Long recipeId, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        recipeLikeRepository.findByRecipeIdAndUserId(recipeId, userId)
                .ifPresent(like -> {
                    recipeLikeRepository.delete(like);
                    
                    Integer currentLikes = recipe.getLikesCount() != null ? recipe.getLikesCount() : 0;
                    recipe.setLikesCount(Math.max(0, currentLikes - 1));
                    recipeRepository.save(recipe);
                });
    }

    public List<CommentResponse> getRecipeComments(Long recipeId) {
        List<Comment> comments = commentRepository.findByRecipeIdOrderByCreatedAtDesc(recipeId);
        return comments.stream()
                .map(comment -> {
                    CommentResponse response = new CommentResponse();
                    response.setId(comment.getId());
                    response.setContent(comment.getContent());
                    response.setUserId(comment.getUser().getId());
                    response.setUsername(comment.getUser().getUsername());
                    response.setCreatedAt(comment.getCreatedAt());
                    return response;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public CommentResponse addComment(Long recipeId, String content, Long userId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = new Comment(content, recipe, user);
        Comment savedComment = commentRepository.save(comment);

        CommentResponse response = new CommentResponse();
        response.setId(savedComment.getId());
        response.setContent(savedComment.getContent());
        response.setUserId(savedComment.getUser().getId());
        response.setUsername(savedComment.getUser().getUsername());
        response.setCreatedAt(savedComment.getCreatedAt());
        
        return response;
    }


    private RecipeResponse convertToResponse(Recipe recipe, Long currentUserId) {
        RecipeResponse response = new RecipeResponse();
        response.setId(recipe.getId());
        response.setTitle(recipe.getTitle());
        response.setDescription(recipe.getDescription());
        response.setPrepTime(recipe.getPrepTime());
        response.setCookTime(recipe.getCookTime());
        response.setTotalTime(recipe.getTotalTime());
        response.setServings(recipe.getServings());
        response.setDifficulty(recipe.getDifficulty().name());
        response.setImageUrl(recipe.getImageUrl());
        response.setIngredients(recipe.getIngredients());
        response.setSteps(recipe.getSteps());
        response.setUserId(recipe.getUser().getId());
        response.setUsername(recipe.getUser().getUsername());
        response.setCreatedAt(recipe.getCreatedAt());
        response.setUpdatedAt(recipe.getUpdatedAt());

        Integer likes = recipe.getLikesCount() != null ? recipe.getLikesCount() : 0;
        Integer views = recipe.getViewsCount() != null ? recipe.getViewsCount() : 0;

        response.setLikesCount(likes);
        response.setViewsCount(views);
        
        if (currentUserId != null) {
            boolean isLiked = recipeLikeRepository.existsByRecipeIdAndUserId(recipe.getId(), currentUserId);
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }

        return response;
    }
}