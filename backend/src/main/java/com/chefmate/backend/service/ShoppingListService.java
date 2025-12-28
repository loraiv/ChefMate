package com.chefmate.backend.service;

import com.chefmate.backend.dto.ShoppingListRequest;
import com.chefmate.backend.dto.ShoppingListResponse;
import com.chefmate.backend.entity.ShoppingList;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.ShoppingListRepository;
import com.chefmate.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class ShoppingListService {

    @Autowired
    private ShoppingListRepository shoppingListRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public ShoppingListResponse createShoppingList(ShoppingListRequest request, Long userId) {
        try {
            // Намери потребителя
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));

            // Създай списък
            ShoppingList shoppingList = new ShoppingList();
            shoppingList.setName(request.getName());
            shoppingList.setUser(user);
            shoppingList.setCreatedAt(LocalDateTime.now());
            shoppingList.setCompleted(false);

            ShoppingList savedList = shoppingListRepository.save(shoppingList);

            // Създай response
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

    // Добавете тези методи, които липсват в ShoppingListController
    public ShoppingListResponse createFromRecipe(Long recipeId, Long userId) {
        // Временна имплементация
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        ShoppingList shoppingList = new ShoppingList();
        shoppingList.setName("Списък от рецепта #" + recipeId);
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
    }

    public List<ShoppingListResponse> getUserShoppingLists(Long userId) {
        List<ShoppingList> lists = shoppingListRepository.findByUserId(userId);
        List<ShoppingListResponse> responses = new ArrayList<>();

        for (ShoppingList list : lists) {
            ShoppingListResponse response = new ShoppingListResponse();
            response.setId(list.getId());
            response.setName(list.getName());
            response.setUsername(list.getUser().getUsername());
            response.setCreatedAt(list.getCreatedAt());
            response.setCompleted(list.getCompleted());
            response.setItems(new ArrayList<>());
            responses.add(response);
        }

        return responses;
    }

    public ShoppingListResponse getShoppingListById(Long listId, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        // Проверка за собственост
        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to access this shopping list");
        }

        ShoppingListResponse response = new ShoppingListResponse();
        response.setId(shoppingList.getId());
        response.setName(shoppingList.getName());
        response.setUsername(shoppingList.getUser().getUsername());
        response.setCreatedAt(shoppingList.getCreatedAt());
        response.setCompleted(shoppingList.getCompleted());
        response.setItems(new ArrayList<>());

        return response;
    }

    @Transactional
    public ShoppingListResponse updateShoppingList(Long listId, ShoppingListRequest request, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        // Проверка за собственост
        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to update this shopping list");
        }

        shoppingList.setName(request.getName());
        ShoppingList updatedList = shoppingListRepository.save(shoppingList);

        ShoppingListResponse response = new ShoppingListResponse();
        response.setId(updatedList.getId());
        response.setName(updatedList.getName());
        response.setUsername(updatedList.getUser().getUsername());
        response.setCreatedAt(updatedList.getCreatedAt());
        response.setCompleted(updatedList.getCompleted());
        response.setItems(new ArrayList<>());

        return response;
    }

    @Transactional
    public void deleteShoppingList(Long listId, Long userId) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        // Проверка за собственост
        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to delete this shopping list");
        }

        shoppingListRepository.delete(shoppingList);
    }

    // Този метод не е нужен засега - коментирайте го или премахнете
    /*
    public ShoppingListItemResponse toggleItemPurchased(Long itemId, Long userId) {
        // TODO: Имплементирай когато имаш ShoppingListItemRepository
        throw new RuntimeException("Not implemented yet");
    }
    */

    @Transactional
    public ShoppingListResponse markListCompleted(Long listId, Long userId, Boolean completed) {
        ShoppingList shoppingList = shoppingListRepository.findById(listId)
                .orElseThrow(() -> new RuntimeException("Shopping list not found"));

        // Проверка за собственост
        if (!shoppingList.getUser().getId().equals(userId)) {
            throw new RuntimeException("Not authorized to modify this list");
        }

        shoppingList.setCompleted(completed);
        ShoppingList updatedList = shoppingListRepository.save(shoppingList);

        ShoppingListResponse response = new ShoppingListResponse();
        response.setId(updatedList.getId());
        response.setName(updatedList.getName());
        response.setUsername(updatedList.getUser().getUsername());
        response.setCreatedAt(updatedList.getCreatedAt());
        response.setCompleted(updatedList.getCompleted());
        response.setItems(new ArrayList<>());

        return response;
    }
}