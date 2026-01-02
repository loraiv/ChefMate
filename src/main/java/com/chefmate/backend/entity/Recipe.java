package com.chefmate.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "prep_time")
    private Integer prepTime;

    @Column(name = "cook_time")
    private Integer cookTime;

    @Column(name = "total_time")
    private Integer totalTime;

    private Integer servings;

    @Enumerated(EnumType.STRING)
    private Difficulty difficulty = Difficulty.EASY;

    @Column(name = "image_url")
    private String imageUrl;

    @ElementCollection
    @CollectionTable(name = "recipe_ingredients", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "ingredient")
    private List<String> ingredients = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "recipe_steps", joinColumns = @JoinColumn(name = "recipe_id"))
    @Column(name = "step", columnDefinition = "TEXT")
    private List<String> steps = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "likes_count")
    private Integer likesCount = 0;

    @Column(name = "views_count")
    private Integer viewsCount = 0;

    public Recipe() {}

    public Recipe(String title, String description, User user) {
        this.title = title;
        this.description = description;
        this.user = user;
    }


    public Long getId() { return id; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getPrepTime() { return prepTime; }
    public Integer getCookTime() { return cookTime; }
    public Integer getTotalTime() { return totalTime; }
    public Integer getServings() { return servings; }
    public Difficulty getDifficulty() { return difficulty; }
    public String getImageUrl() { return imageUrl; }
    public List<String> getIngredients() { return ingredients; }
    public List<String> getSteps() { return steps; }
    public User getUser() { return user; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public Integer getLikesCount() { return likesCount; }
    public Integer getViewsCount() { return viewsCount; }

    public void setId(Long id) { this.id = id; }
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrepTime(Integer prepTime) {
        this.prepTime = prepTime;
        calculateTotalTime();
    }
    public void setCookTime(Integer cookTime) {
        this.cookTime = cookTime;
        calculateTotalTime();
    }
    public void setTotalTime(Integer totalTime) { this.totalTime = totalTime; }
    public void setServings(Integer servings) { this.servings = servings; }
    public void setDifficulty(Difficulty difficulty) { this.difficulty = difficulty; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public void setUser(User user) { this.user = user; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }
    public void setViewsCount(Integer viewsCount) { this.viewsCount = viewsCount; }

    // Helper method
    private void calculateTotalTime() {
        int prep = (prepTime != null) ? prepTime : 0;
        int cook = (cookTime != null) ? cookTime : 0;
        this.totalTime = prep + cook;
    }

    public enum Difficulty {
        EASY, MEDIUM, HARD
    }
    
}