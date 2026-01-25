package com.chefmate.backend.controller;

import com.chefmate.backend.dto.ShoppingListItemRequest;
import com.chefmate.backend.dto.ShoppingListItemResponse;
import com.chefmate.backend.dto.ShoppingListRequest;
import com.chefmate.backend.dto.ShoppingListResponse;
import com.chefmate.backend.entity.ShoppingListItem;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.ShoppingListService;
import com.chefmate.backend.utils.JwtUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/shopping-lists")
public class ShoppingListController {

    private final ShoppingListService shoppingListService;
    private final JwtService jwtService;

    public ShoppingListController(ShoppingListService shoppingListService, JwtService jwtService) {
        this.shoppingListService = shoppingListService;
        this.jwtService = jwtService;
    }

    @PostMapping
    public ResponseEntity<ShoppingListResponse> createShoppingList(
            @RequestBody ShoppingListRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        ShoppingListResponse response = shoppingListService.createShoppingList(request, userId);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/create-from-recipe/{recipeId}")
    public ResponseEntity<ShoppingListResponse> createFromRecipe(
            @PathVariable Long recipeId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ShoppingListResponse response = shoppingListService.createFromRecipe(recipeId, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping("/my-list")
    public ResponseEntity<ShoppingListResponse> getMyShoppingList(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ShoppingListResponse response = shoppingListService.getMyShoppingList(userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(404).build();
        }
    }

    @GetMapping
    public ResponseEntity<List<ShoppingListResponse>> getUserShoppingLists(
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        List<ShoppingListResponse> lists = shoppingListService.getUserShoppingLists(userId);
        return ResponseEntity.ok(lists);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> getShoppingList(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ShoppingListResponse response = shoppingListService.getShoppingListById(id, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<ShoppingListResponse> updateShoppingList(
            @PathVariable Long id,
            @RequestBody ShoppingListRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ShoppingListResponse response = shoppingListService.updateShoppingList(id, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteShoppingList(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            shoppingListService.deleteShoppingList(id, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PostMapping("/{listId}/items")
    public ResponseEntity<ShoppingListItemResponse> addShoppingListItem(
            @PathVariable Long listId,
            @RequestBody ShoppingListItemRequest request,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ShoppingListItemResponse response = shoppingListService.addShoppingListItem(listId, request, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PutMapping("/{listId}/items/{itemId}")
    public ResponseEntity<ShoppingListItemResponse> updateShoppingListItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @RequestBody Map<String, Object> itemUpdate,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ShoppingListItem itemEntity = new ShoppingListItem();
            if (itemUpdate.containsKey("name")) {
                itemEntity.setName((String) itemUpdate.get("name"));
            }
            if (itemUpdate.containsKey("quantity")) {
                itemEntity.setQuantity(itemUpdate.get("quantity") != null ? itemUpdate.get("quantity").toString() : null);
            }
            if (itemUpdate.containsKey("unit")) {
                itemEntity.setUnit((String) itemUpdate.get("unit"));
            }
            if (itemUpdate.containsKey("checked")) {
                Object checked = itemUpdate.get("checked");
                if (checked instanceof Boolean) {
                    itemEntity.setPurchased((Boolean) checked);
                } else if (checked instanceof String) {
                    itemEntity.setPurchased(Boolean.parseBoolean((String) checked));
                }
            } else if (itemUpdate.containsKey("purchased")) {
                Object purchased = itemUpdate.get("purchased");
                if (purchased instanceof Boolean) {
                    itemEntity.setPurchased((Boolean) purchased);
                } else if (purchased instanceof String) {
                    itemEntity.setPurchased(Boolean.parseBoolean((String) purchased));
                }
            }
            
            ShoppingListItemResponse response = shoppingListService.updateShoppingListItem(listId, itemId, itemEntity, userId);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @DeleteMapping("/{listId}/items/{itemId}")
    public ResponseEntity<Void> deleteShoppingListItem(
            @PathVariable Long listId,
            @PathVariable Long itemId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            shoppingListService.deleteShoppingListItem(listId, itemId, userId);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }

    @PatchMapping("/{id}/complete")
    public ResponseEntity<ShoppingListResponse> markListCompleted(
            @PathVariable Long id,
            @RequestParam Boolean completed,
            @RequestHeader(value = "Authorization", required = false) String token) {
        Long userId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (userId == null) {
            return ResponseEntity.status(401).build();
        }
        try {
            ShoppingListResponse response = shoppingListService.markListCompleted(id, userId, completed);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(403).build();
        }
    }
}
