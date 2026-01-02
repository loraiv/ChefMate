package com.chefmate.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class RecipeResponse {
    private Long id;
    private String title;
    private String description;
    private Integer prepTime;
    private Integer cookTime;
    private Integer totalTime;
    private Integer servings;
    private String difficulty;
    private String imageUrl;
    private List<String> ingredients;
    private List<String> steps;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer likesCount;
    private Integer viewsCount;
    private Boolean isLiked = false;

    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getPrepTime() { return prepTime; }
    public Integer getCookTime() { return cookTime; }
    public Integer getTotalTime() { return totalTime; }
    public Integer getServings() { return servings; }
    public String getDifficulty() { return difficulty; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getIngredients() { return ingredients; }
    public List<String> getSteps() { return steps; }
    public Long getUserId() { return userId; }
    public String getUsername() { return username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getLikesCount() { return likesCount; }
    public Integer getViewsCount() { return viewsCount; }
    public Boolean getIsLiked() { return isLiked; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrepTime(Integer prepTime) { this.prepTime = prepTime; }
    public void setCookTime(Integer cookTime) { this.cookTime = cookTime; }
    public void setTotalTime(Integer totalTime) { this.totalTime = totalTime; }
    public void setServings(Integer servings) { this.servings = servings; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public void setUserId(Long userId) { this.userId = userId; }
    public void setUsername(String username) { this.username = username; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }
    public void setViewsCount(Integer viewsCount) { this.viewsCount = viewsCount; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }
}