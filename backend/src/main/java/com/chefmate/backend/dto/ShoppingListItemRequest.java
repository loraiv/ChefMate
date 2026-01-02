package com.chefmate.backend.dto;

public class ShoppingListItemRequest {
    private String name;
    private String quantity;
    private String unit;
    private Long recipeId;

    // Getters
    public String getName() { return name; }
    public String getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Long getRecipeId() { return recipeId; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
}