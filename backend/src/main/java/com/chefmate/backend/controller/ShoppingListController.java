package com.chefmate.backend.controller;

import com.chefmate.backend.dto.ShoppingListItemResponse;
import com.chefmate.backend.dto.ShoppingListRequest;
import com.chefmate.backend.dto.ShoppingListResponse;
import com.chefmate.backend.service.ShoppingListService;
import com.chefmate.backend.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    @Autowired
    private ShoppingListService shoppingListService;

    @Autowired
    private JwtService jwtService;

    @PostMapping
    public ResponseEntity<ShoppingListResponse> createShoppingList(
            @RequestBody ShoppingListRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        ShoppingListResponse response = shoppingListService.createShoppingList(request, userId);
        return ResponseEntity.ok(response);
    }

    // @PostMapping("/from-recipe/{recipeId}")
    // public ResponseEntity<ShoppingListResponse> createFromRecipe(...) { ... }

    @GetMapping
    public ResponseEntity<List<ShoppingListResponse>> getUserShoppingLists(
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        List<ShoppingListResponse> lists = shoppingListService.getUserShoppingLists(userId);
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> getShoppingList(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        ShoppingListResponse response = shoppingListService.getShoppingListById(id, userId);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> updateShoppingList(
            @PathVariable Long id,
            @RequestBody ShoppingListRequest request,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        ShoppingListResponse response = shoppingListService.updateShoppingList(id, request, userId);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShoppingList(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        shoppingListService.deleteShoppingList(id, userId);
        return ResponseEntity.noContent().build();
    }

    // КОМЕНТИРАЙ засега - нямаме този метод в Service:
    /*
    @PatchMapping("/items/{itemId}/toggle")
    public ResponseEntity<ShoppingListItemResponse> toggleItemPurchased(
            @PathVariable Long itemId,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        ShoppingListItemResponse response = shoppingListService.toggleItemPurchased(itemId, userId);
        return ResponseEntity.ok(response);
    }
    */

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ShoppingListResponse> markListCompleted(
            @PathVariable Long id,
            @RequestParam Boolean completed,
            @RequestHeader("Authorization") String token) {
        Long userId = getUserIdFromToken(token);
        ShoppingListResponse response = shoppingListService.markListCompleted(id, userId, completed);
        return ResponseEntity.ok(response);
    }

    private Long getUserIdFromToken(String token) {
        if (token != null && token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        return 1L;
    }
}