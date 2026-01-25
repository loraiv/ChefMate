package com.chefmate.backend.service;

import com.chefmate.backend.dto.CommentResponse;
import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.entity.Comment;
import com.chefmate.backend.entity.Recipe;
import com.chefmate.backend.entity.RecipeLike;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.CommentLikeRepository;
import com.chefmate.backend.repository.CommentRepository;
import com.chefmate.backend.repository.RecipeLikeRepository;
import com.chefmate.backend.repository.RecipeRepository;
import com.chefmate.backend.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class RecipeService {

    private static final Logger logger = LoggerFactory.getLogger(RecipeService.class);

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;
    private final RecipeLikeRepository recipeLikeRepository;
    private final CommentRepository commentRepository;
    private final CommentLikeRepository commentLikeRepository;

    public RecipeService(RecipeRepository recipeRepository,
                         UserRepository userRepository,
                         RecipeLikeRepository recipeLikeRepository,
                         CommentRepository commentRepository,
                         CommentLikeRepository commentLikeRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
        this.recipeLikeRepository = recipeLikeRepository;
        this.commentRepository = commentRepository;
        this.commentLikeRepository = commentLikeRepository;
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

    @Transactional
    public RecipeResponse createRecipeWithImage(RecipeRequest request, Long userId, String imageUrl) {
        List<String> imageUrls = imageUrl != null ? List.of(imageUrl) : new ArrayList<>();
        return createRecipeWithImages(request, userId, imageUrls);
    }

    @Transactional
    public RecipeResponse createRecipeWithImages(RecipeRequest request, Long userId, List<String> imageUrls) {
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
        
        // Set all image URLs
        if (imageUrls != null && !imageUrls.isEmpty()) {
            recipe.setImageUrls(imageUrls);
            // Set first image URL for backward compatibility (single imageUrl field)
            recipe.setImageUrl(imageUrls.get(0));
        }

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
            throw new RuntimeException("You are not authorized to update this recipe");
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
    public RecipeResponse updateRecipeWithImages(Long id, RecipeRequest request, Long userId, List<String> imageUrls) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + id));

        if (!recipe.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to update this recipe");
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
        
        // Update images
        if (imageUrls != null && !imageUrls.isEmpty()) {
            recipe.setImageUrls(imageUrls);
            recipe.setImageUrl(imageUrls.get(0)); // Set first image for backward compatibility
        }
        
        recipe.setUpdatedAt(LocalDateTime.now());

        Recipe updatedRecipe = recipeRepository.save(recipe);
        return convertToResponse(updatedRecipe, userId);
    }

    @Transactional
    public void deleteRecipe(Long id, Long userId) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + id));

        if (!recipe.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this recipe");
        }

        // Delete all related data first
        // Delete all likes for this recipe
        recipeLikeRepository.deleteByRecipeId(id);
        
        // Delete all comments for this recipe
        commentRepository.deleteByRecipeId(id);
        
        // Delete the recipe (this will also delete ingredients, steps, and imageUrls via cascade)
        recipeRepository.delete(recipe);
    }

    public List<RecipeResponse> searchRecipes(String query, String difficulty, Integer maxTime, Long currentUserId) {
        List<Recipe> recipes = recipeRepository.findAll();
        
        return recipes.stream()
                .filter(recipe -> {
                    if (query != null && !query.isEmpty()) {
                        String queryLower = query.toLowerCase();
                        String titleLower = recipe.getTitle().toLowerCase();
                        String descriptionLower = recipe.getDescription() != null ? recipe.getDescription().toLowerCase() : "";
                        String usernameLower = recipe.getUser() != null && recipe.getUser().getUsername() != null 
                                ? recipe.getUser().getUsername().toLowerCase() : "";
                        
                        if (!titleLower.contains(queryLower) &&
                            !descriptionLower.contains(queryLower) &&
                            !usernameLower.contains(queryLower)) {
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
        return getRecipeComments(recipeId, null);
    }

    public List<CommentResponse> getRecipeComments(Long recipeId, Long currentUserId) {
        // Get only top-level comments (no parent) with users eagerly loaded
        List<Comment> topLevelComments = commentRepository.findByRecipeIdAndParentCommentIsNullOrderByCreatedAtDesc(recipeId);
        
        if (topLevelComments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Collect all comment IDs for batch loading (recursively load all replies)
        List<Long> allCommentIds = new ArrayList<>();
        List<Comment> allReplies = new ArrayList<>();
        
        // Recursively load all replies (including nested replies)
        loadAllRepliesRecursively(topLevelComments, allReplies, allCommentIds);
        
        // Add top-level comment IDs
        topLevelComments.forEach(comment -> allCommentIds.add(comment.getId()));
        
        logger.debug("Loading comments for recipe {}, found {} top-level comments, {} total replies (including nested)", 
                    recipeId, topLevelComments.size(), allReplies.size());
        
        // Group replies by parent comment ID
        Map<Long, List<Comment>> repliesByParent = allReplies.stream()
                .collect(Collectors.groupingBy(reply -> reply.getParentComment().getId()));
        
        Map<Long, Integer> likesCountByComment = new HashMap<>();
        Map<Long, Boolean> isLikedByComment = new HashMap<>();
        
        // Initialize all comment IDs with 0 likes and false isLiked
        for (Long commentId : allCommentIds) {
            likesCountByComment.put(commentId, 0);
            isLikedByComment.put(commentId, false);
        }
        
        // Load all likes at once using batch query
        if (!allCommentIds.isEmpty()) {
            List<com.chefmate.backend.entity.CommentLike> allLikes = commentLikeRepository.findByCommentIds(allCommentIds);
            
            // Count likes per comment
            for (com.chefmate.backend.entity.CommentLike like : allLikes) {
                Long commentId = like.getComment().getId();
                likesCountByComment.put(commentId, likesCountByComment.get(commentId) + 1);
                
                // Check if current user liked this comment
                if (currentUserId != null && like.getUser().getId().equals(currentUserId)) {
                    isLikedByComment.put(commentId, true);
                }
            }
        }
        
        // Convert to responses using pre-loaded data (recursively)
        return topLevelComments.stream()
                .map(comment -> convertCommentToResponseRecursive(
                        comment, 
                        currentUserId, 
                        repliesByParent,
                        likesCountByComment,
                        isLikedByComment))
                .collect(Collectors.toList());
    }
    
    private void loadAllRepliesRecursively(List<Comment> comments, List<Comment> allReplies, List<Long> allCommentIds) {
        if (comments.isEmpty()) {
            return;
        }
        
        // Get IDs of current level comments
        List<Long> currentLevelIds = comments.stream()
                .map(Comment::getId)
                .collect(Collectors.toList());
        
        // Load replies for current level
        List<Comment> replies = commentRepository.findByParentCommentIdsOrderByCreatedAtAsc(currentLevelIds);
        
        if (!replies.isEmpty()) {
            allReplies.addAll(replies);
            replies.forEach(reply -> allCommentIds.add(reply.getId()));
            
            // Recursively load replies to replies
            loadAllRepliesRecursively(replies, allReplies, allCommentIds);
        }
    }

    private CommentResponse convertCommentToResponse(Comment comment, Long currentUserId) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUserId(comment.getUser().getId());
        response.setUsername(comment.getUser().getUsername());
        response.setCreatedAt(comment.getCreatedAt());
        
        // Set parent comment ID if it's a reply
        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getId());
        }
        
        // Get replies
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(comment.getId());
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> convertCommentToResponse(reply, currentUserId))
                .collect(Collectors.toList());
        response.setReplies(replyResponses);
        
        // Get likes count
        int likesCount = commentLikeRepository.findByCommentId(comment.getId()).size();
        response.setLikesCount(likesCount);
        
        // Check if current user liked this comment
        if (currentUserId != null) {
            boolean isLiked = commentLikeRepository.existsByCommentIdAndUserId(comment.getId(), currentUserId);
            response.setIsLiked(isLiked);
        } else {
            response.setIsLiked(false);
        }
        
        // Set user profile image URL
        String profileImageUrl = comment.getUser().getProfileImageUrl();
        response.setUserProfileImageUrl(profileImageUrl != null ? profileImageUrl : null);
        
        return response;
    }
    
    private CommentResponse convertCommentToResponseRecursive(
            Comment comment, 
            Long currentUserId,
            Map<Long, List<Comment>> repliesByParent,
            Map<Long, Integer> likesCountByComment,
            Map<Long, Boolean> isLikedByComment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUserId(comment.getUser().getId());
        response.setUsername(comment.getUser().getUsername());
        response.setCreatedAt(comment.getCreatedAt());
        
        // Set parent comment ID if it's a reply
        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getId());
        }
        
        // Get replies for this comment
        List<Comment> replies = repliesByParent.getOrDefault(comment.getId(), new ArrayList<>());
        
        // Convert replies recursively (including nested replies)
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> convertCommentToResponseRecursive(
                        reply,
                        currentUserId,
                        repliesByParent,
                        likesCountByComment,
                        isLikedByComment))
                .collect(Collectors.toList());
        response.setReplies(replyResponses);
        
        // Get likes count from pre-loaded map
        response.setLikesCount(likesCountByComment.getOrDefault(comment.getId(), 0));
        
        // Get isLiked from pre-loaded map
        response.setIsLiked(isLikedByComment.getOrDefault(comment.getId(), false));
        
        // Set user profile image URL
        String profileImageUrl = comment.getUser().getProfileImageUrl();
        response.setUserProfileImageUrl(profileImageUrl != null ? profileImageUrl : null);
        
        return response;
    }
    
    private CommentResponse convertCommentToResponseOptimized(
            Comment comment, 
            Long currentUserId,
            List<Comment> replies,
            Map<Long, Integer> likesCountByComment,
            Map<Long, Boolean> isLikedByComment) {
        CommentResponse response = new CommentResponse();
        response.setId(comment.getId());
        response.setContent(comment.getContent());
        response.setUserId(comment.getUser().getId());
        response.setUsername(comment.getUser().getUsername());
        response.setCreatedAt(comment.getCreatedAt());
        
        // Set parent comment ID if it's a reply
        if (comment.getParentComment() != null) {
            response.setParentCommentId(comment.getParentComment().getId());
        }
        
        // Convert replies using pre-loaded data
        List<CommentResponse> replyResponses = replies.stream()
                .map(reply -> {
                    CommentResponse replyResponse = new CommentResponse();
                    replyResponse.setId(reply.getId());
                    replyResponse.setContent(reply.getContent());
                    replyResponse.setUserId(reply.getUser().getId());
                    replyResponse.setUsername(reply.getUser().getUsername());
                    replyResponse.setCreatedAt(reply.getCreatedAt());
                    replyResponse.setParentCommentId(reply.getParentComment().getId());
                    replyResponse.setLikesCount(likesCountByComment.getOrDefault(reply.getId(), 0));
                    replyResponse.setIsLiked(isLikedByComment.getOrDefault(reply.getId(), false));
                    String replyProfileImageUrl = reply.getUser().getProfileImageUrl();
                    replyResponse.setUserProfileImageUrl(replyProfileImageUrl != null ? replyProfileImageUrl : null);
                    replyResponse.setReplies(new ArrayList<>()); // Replies don't have nested replies
                    return replyResponse;
                })
                .collect(Collectors.toList());
        response.setReplies(replyResponses);
        
        // Get likes count from pre-loaded map
        response.setLikesCount(likesCountByComment.getOrDefault(comment.getId(), 0));
        
        // Get isLiked from pre-loaded map
        response.setIsLiked(isLikedByComment.getOrDefault(comment.getId(), false));
        
        // Set user profile image URL
        String profileImageUrl = comment.getUser().getProfileImageUrl();
        response.setUserProfileImageUrl(profileImageUrl != null ? profileImageUrl : null);
        
        return response;
    }

    @Transactional
    public CommentResponse addComment(Long recipeId, String content, Long userId) {
        return addComment(recipeId, content, userId, null);
    }

    @Transactional
    public CommentResponse addComment(Long recipeId, String content, Long userId, Long parentCommentId) {
        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Comment comment = new Comment(content, recipe, user);
        
        // If it's a reply, set parent comment
        if (parentCommentId != null) {
            Comment parentComment = commentRepository.findById(parentCommentId)
                    .orElseThrow(() -> new RuntimeException("Parent comment not found"));
            comment.setParentComment(parentComment);
            logger.debug("Creating reply to comment {} for recipe {}", parentCommentId, recipeId);
        } else {
            logger.debug("Creating top-level comment for recipe {}", recipeId);
        }
        
        // Use saveAndFlush to ensure the comment is immediately persisted to the database
        // This is important so that when we reload comments, the new reply is included
        Comment savedComment = commentRepository.saveAndFlush(comment);
        logger.debug("Saved comment with ID {} {}", savedComment.getId(), 
                    parentCommentId != null ? "(reply to " + parentCommentId + ")" : "(top-level)");
        return convertCommentToResponse(savedComment, userId);
    }

    @Transactional
    public CommentResponse replyToComment(Long commentId, String content, Long userId) {
        Comment parentComment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        Long recipeId = parentComment.getRecipe().getId();
        return addComment(recipeId, content, userId, commentId);
    }

    @Transactional
    public void likeComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            return; // Already liked
        }

        com.chefmate.backend.entity.CommentLike like = new com.chefmate.backend.entity.CommentLike(comment, user);
        commentLikeRepository.save(like);
    }

    @Transactional
    public void unlikeComment(Long commentId, Long userId) {
        commentLikeRepository.findByCommentIdAndUserId(commentId, userId)
                .ifPresent(like -> commentLikeRepository.delete(like));
    }

    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        // Check if user is the owner of the comment
        if (!comment.getUser().getId().equals(userId)) {
            throw new RuntimeException("You are not authorized to delete this comment");
        }

        deleteCommentInternal(commentId, "user");
    }

    @Transactional
    public void deleteCommentAsAdmin(Long commentId) {
        // Admins can delete any comment regardless of owner
        commentRepository.findById(commentId)
                .orElseThrow(() -> new RuntimeException("Comment not found"));

        deleteCommentInternal(commentId, "admin");
    }

    private void deleteCommentInternal(Long commentId, String actor) {
        logger.debug("Deleting comment {} with all replies (actor: {})", commentId, actor);

        // Recursively delete all replies (including nested replies) and their likes
        int deletedCount = deleteCommentRecursively(commentId);
        logger.debug("Deleted {} nested replies for comment {}", deletedCount, commentId);

        // Delete all likes for this comment
        commentLikeRepository.deleteByCommentId(commentId);

        // Delete the comment itself
        commentRepository.deleteById(commentId);

        // Flush to ensure all deletions are persisted
        commentRepository.flush();

        logger.debug("Comment {} deleted successfully (actor: {})", commentId, actor);
    }
    
    private int deleteCommentRecursively(Long commentId) {
        // Get all direct replies to this comment
        List<Comment> replies = commentRepository.findByParentCommentIdOrderByCreatedAtAsc(commentId);
        int count = 0;
        
        // Recursively delete each reply and its nested replies
        for (Comment reply : replies) {
            // Recursively delete nested replies first
            count += deleteCommentRecursively(reply.getId());
            
            // Delete all likes for this reply
            commentLikeRepository.deleteByCommentId(reply.getId());
            
            // Delete the reply itself
            commentRepository.delete(reply);
            count++;
        }
        
        return count;
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
        // Set all image URLs if available, otherwise use single imageUrl
        if (recipe.getImageUrls() != null && !recipe.getImageUrls().isEmpty()) {
            response.setImageUrls(recipe.getImageUrls());
        } else if (recipe.getImageUrl() != null) {
            response.setImageUrls(List.of(recipe.getImageUrl()));
        } else {
            response.setImageUrls(new ArrayList<>());
        }
        response.setIngredients(recipe.getIngredients());
        response.setSteps(recipe.getSteps());
        response.setUserId(recipe.getUser().getId());
        response.setUsername(recipe.getUser().getUsername());
        response.setUserProfileImageUrl(recipe.getUser().getProfileImageUrl());
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