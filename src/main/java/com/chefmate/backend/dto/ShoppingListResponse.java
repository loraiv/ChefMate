package com.chefmate.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class ShoppingListResponse {
    private Long id;
    private String name;
    private String username;
    private LocalDateTime createdAt;
    private Boolean completed;
    private List<ShoppingListItemResponse> items;

    // Getters
    public Long getId() { return id; }
    public String getName() { return name; }
    public String getUsername() { return username; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public Boolean getCompleted() { return completed; }
    public List<ShoppingListItemResponse> getItems() { return items; }

    // Setters
    public void setId(Long id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setUsername(String username) { this.username = username; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public void setCompleted(Boolean completed) { this.completed = completed; }
    public void setItems(List<ShoppingListItemResponse> items) { this.items = items; }
}