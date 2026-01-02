package com.chefmate.backend.dto;

import java.util.List;

public class ShoppingListRequest {
    private String name;
    private List<ShoppingListItemRequest> items;

    // Getters
    public String getName() { return name; }
    public List<ShoppingListItemRequest> getItems() { return items; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setItems(List<ShoppingListItemRequest> items) { this.items = items; }
}