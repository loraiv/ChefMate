package com.chefmate.backend.service;

import com.chefmate.backend.dto.ShoppingListItemResponse;
import com.chefmate.backend.dto.ShoppingListRequest;
import com.chefmate.backend.dto.ShoppingListResponse;
import com.chefmate.backend.entity.Recipe;
import com.chefmate.backend.entity.ShoppingList;
import com.chefmate.backend.entity.ShoppingListItem;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.RecipeRepository;
import com.chefmate.backend.repository.ShoppingListItemRepository;
import com.chefmate.backend.repository.ShoppingListRepository;
import com.chefmate.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ShoppingListService {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ShoppingListItemRepository shoppingListItemRepository;

    @Autowired
    private RecipeRepository recipeRepository;

    @Transactional
    public ShoppingListResponse createShoppingList(ShoppingListRequest request, Long userId) {
        try {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            ShoppingList shoppingList = new ShoppingList();
            shoppingList.setName(request.getName());
            shoppingList.setUser(user);
            shoppingList.setCreatedAt(LocalDateTime.now());
            shoppingList.setCompleted(false);

            ShoppingList savedList = shoppingListRepository.save(shoppingList);

            ShoppingListResponse response = new ShoppingListResponse();
            response.setId(savedList.getId());
            response.setName(savedList.getName());
            response.setUsername(savedList.getUser().getUsername());
            response.setCreatedAt(savedList.getCreatedAt());
            response.setCompleted(savedList.getCompleted());
            response.setItems(new ArrayList<>());

            return response;
        } catch (Exception e) {
            throw new RuntimeException("Error creating shopping list: " + e.getMessage());
        }
    }

    @Transactional
    public ShoppingListResponse createFromRecipe(Long recipeId, Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        Recipe recipe = recipeRepository.findById(recipeId)
                .orElseThrow(() -> new RuntimeException("Recipe not found"));

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Списък от: " + recipe.getTitle());
        shoppingList.setUser(user);
        shoppingList.setCreatedAt(LocalDateTime.now());
        shoppingList.setCompleted(false);

        ShoppingList savedList = shoppingListRepository.save(shoppingList);

        List<ShoppingListItem> items = new ArrayList<>();
        if (recipe.getIngredients() != null) {
            for (String ingredient : recipe.getIngredients()) {
                ShoppingListItem item = new ShoppingListItem();
                item.setName(ingredient);
                item.setQuantity("");
                item.setUnit("");
                item.setPurchased(false);
                item.setShoppingList(savedList);
                item.setRecipe(recipe);
                items.add(item);
            }
        }
        shoppingListItemRepository.saveAll(items);

        return convertToResponse(savedList);
    }

    public ShoppingListResponse getMyShoppingList(Long userId) {
        List<ShoppingList> activeLists = shoppingListRepository.findByUserIdAndCompletedOrderByCreatedAtDesc(userId, false);
        
        if (activeLists.isEmpty()) {
            ShoppingList newList = new ShoppingList();
            newList.setName("Моят списък");
            newList.setUser(userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found")));
            newList.setCreatedAt(LocalDateTime.now());
            newList.setCompleted(false);
            ShoppingList savedList = shoppingListRepository.save(newList);
            return convertToResponse(savedList);
        }

        // Return the most recent active list (first in the sorted list)
        return convertToResponse(activeLists.get(0));
    }

    public List<ShoppingListResponse> getUserShoppingLists(Long userId) {
        List<ShoppingList> lists = shoppingListRepository.findByUserId(userId);
        return lists.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    public ShoppingListResponse getShoppingListById(Long listId, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to access this shopping list");
        }

        return convertToResponse(shoppingList);
    }

    @Transactional
    public ShoppingListResponse updateShoppingList(Long listId, ShoppingListRequest request, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this shopping list");
        }

        shoppingList.setName(request.getName());
        ShoppingList updatedList = shoppingListRepository.save(shoppingList);

        return convertToResponse(updatedList);
    }

    @Transactional
    public void deleteShoppingList(Long listId, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this shopping list");
        }

        shoppingListRepository.delete(shoppingList);
    }


    @Transactional
    public ShoppingListResponse markListCompleted(Long listId, Long userId, Boolean completed) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this list");
        }

        shoppingList.setCompleted(completed);
        ShoppingList updatedList = shoppingListRepository.save(shoppingList);

        return convertToResponse(updatedList);
    }

    @Transactional
    public ShoppingListItemResponse updateShoppingListItem(Long listId, Long itemId, com.chefmate.backend.entity.ShoppingListItem itemUpdate, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this list");
        }

        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        if (itemUpdate.getName() != null) item.setName(itemUpdate.getName());
        if (itemUpdate.getQuantity() != null) item.setQuantity(itemUpdate.getQuantity());
        if (itemUpdate.getUnit() != null) item.setUnit(itemUpdate.getUnit());
        if (itemUpdate.getPurchased() != null) item.setPurchased(itemUpdate.getPurchased());

        ShoppingListItem updatedItem = shoppingListItemRepository.save(item);
        return convertItemToResponse(updatedItem);
    }

    @Transactional
    public void deleteShoppingListItem(Long listId, Long itemId, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this list");
        }

        ShoppingListItem item = shoppingListItemRepository.findById(itemId)
                .orElseThrow(() -> new RuntimeException("Item not found"));

        shoppingListItemRepository.delete(item);
    }

    private ShoppingListResponse convertToResponse(ShoppingList shoppingList) {
        ShoppingListResponse response = new ShoppingListResponse();
        response.setId(shoppingList.getId());
        response.setName(shoppingList.getName());
        response.setUsername(shoppingList.getUser().getUsername());
        response.setCreatedAt(shoppingList.getCreatedAt());
        response.setCompleted(shoppingList.getCompleted());

        List<ShoppingListItem> items = shoppingListItemRepository.findByShoppingListId(shoppingList.getId());
        response.setItems(items.stream()
                .map(this::convertItemToResponse)
                .collect(Collectors.toList()));

        return response;
    }

    private ShoppingListItemResponse convertItemToResponse(ShoppingListItem item) {
        ShoppingListItemResponse response = new ShoppingListItemResponse();
        response.setId(item.getId());
        response.setName(item.getName());
        response.setQuantity(item.getQuantity());
        response.setUnit(item.getUnit());
        response.setPurchased(item.getPurchased());
        
        if (item.getRecipe() != null) {
            response.setRecipeId(item.getRecipe().getId());
            response.setRecipeName(item.getRecipe().getTitle());
        }

        return response;
    }
}