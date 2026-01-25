package com.chefmate.backend.dto;

import java.util.List;

public class RecipeRequest {
    private String title;
    private String description;
    private Integer prepTime;
    private Integer cookTime;
    private Integer servings;
    private String difficulty;
    private List<String> ingredients;
    private List<String> steps;

    // Getters
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public Integer getPrepTime() { return prepTime; }
    public Integer getCookTime() { return cookTime; }
    public Integer getServings() { return servings; }
    public String getDifficulty() { return difficulty; }
    public List<String> getIngredients() { return ingredients; }
    public List<String> getSteps() { return steps; }

    // Setters
    public void setTitle(String title) { this.title = title; }
    public void setDescription(String description) { this.description = description; }
    public void setPrepTime(Integer prepTime) { this.prepTime = prepTime; }
    public void setCookTime(Integer cookTime) { this.cookTime = cookTime; }
    public void setServings(Integer servings) { this.servings = servings; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public void setIngredients(List<String> ingredients) { this.ingredients = ingredients; }
    public void setSteps(List<String> steps) { this.steps = steps; }
}