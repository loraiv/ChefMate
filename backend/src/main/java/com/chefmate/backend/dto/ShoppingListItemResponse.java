package com.chefmate.backend.dto;

public class ShoppingListItemResponse {
    private Long id;
    private String name;
    private String quantity;
    private String unit;
    private Boolean purchased;
    private Long recipeId;
    private String recipeName;

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getQuantity() { return quantity; }
    public String getUnit() { return unit; }
    public Boolean getPurchased() { return purchased; }
    public Long getRecipeId() { return recipeId; }
    public String getRecipeName() { return recipeName; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setQuantity(String quantity) { this.quantity = quantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setPurchased(Boolean purchased) { this.purchased = purchased; }
    public void setRecipeId(Long recipeId) { this.recipeId = recipeId; }
    public void setRecipeName(String recipeName) { this.recipeName = recipeName; }
}