package com.chefmate.backend.service;

import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.entity.Recipe;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.RecipeRepository;
import com.chefmate.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class RecipeService {

    private final RecipeRepository recipeRepository;
    private final UserRepository userRepository;

    // Времено без FileStorageService
    public RecipeService(RecipeRepository recipeRepository,
                         UserRepository userRepository) {
        this.recipeRepository = recipeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public RecipeResponse createRecipe(RecipeRequest request, Long userId) {
        // Намери потребителя
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + userId));

        // Създай нова рецепта
        Recipe recipe = new Recipe();
        recipe.setTitle(request.getTitle());
        recipe.setDescription(request.getDescription());
        recipe.setPrepTime(request.getPrepTime());
        recipe.setCookTime(request.getCookTime());

        // Автоматично изчисли общо време
        Integer totalTime = 0;
        if (request.getPrepTime() != null) {
            totalTime += request.getPrepTime();
        }
        if (request.getCookTime() != null) {
            totalTime += request.getCookTime();
        }
        recipe.setTotalTime(totalTime);

        recipe.setServings(request.getServings());

// В createRecipe метода, променете тази част:
// Конвертирай difficulty от String към enum
        if (request.getDifficulty() != null) {
            try {
                // Използвайте правилния път към Difficulty
                Recipe.Difficulty difficultyEnum = Recipe.Difficulty.valueOf(
                        request.getDifficulty().toUpperCase()
                );
                recipe.setDifficulty(difficultyEnum);
            } catch (IllegalArgumentException e) {
                recipe.setDifficulty(Recipe.Difficulty.EASY);
            }
        } else {
            recipe.setDifficulty(Recipe.Difficulty.EASY);
        }
        recipe.setIngredients(request.getIngredients() != null ?
                request.getIngredients() : new ArrayList<>());
        recipe.setSteps(request.getSteps() != null ?
                request.getSteps() : new ArrayList<>());
        recipe.setUser(user);

        // Запази рецептата
        Recipe savedRecipe = recipeRepository.save(recipe);

        // Върни response
        return convertToResponse(savedRecipe);
    }

    public List<RecipeResponse> getAllRecipes() {
        List<Recipe> recipes = recipeRepository.findAll();
        List<RecipeResponse> responses = new ArrayList<>();

        for (Recipe recipe : recipes) {
            responses.add(convertToResponse(recipe));
        }

        return responses;
    }

    public RecipeResponse getRecipeById(Long id) {
        Recipe recipe = recipeRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Recipe not found with ID: " + id));

        // Увеличи брояча на прегледи
        Integer currentViews = recipe.getViewsCount() != null ? recipe.getViewsCount() : 0;
        recipe.setViewsCount(currentViews + 1);
        recipeRepository.save(recipe);

        return convertToResponse(recipe);
    }

    public List<RecipeResponse> getUserRecipes(Long userId) {
        List<Recipe> recipes = recipeRepository.findByUserId(userId);
        List<RecipeResponse> responses = new ArrayList<>();

        for (Recipe recipe : recipes) {
            responses.add(convertToResponse(recipe));
        }

        return responses;
    }

    // Времено коментирай методите за снимки
    /*
    public RecipeResponse uploadRecipeImage(Long recipeId, MultipartFile file) throws IOException {
        // TODO: Имплементирай когато имаш FileStorageService
        throw new RuntimeException("Not implemented yet");
    }

    public void deleteRecipeImage(Long recipeId) throws IOException {
        // TODO: Имплементирай когато имаш FileStorageService
        throw new RuntimeException("Not implemented yet");
    }
    */

    private RecipeResponse convertToResponse(Recipe recipe) {
        RecipeResponse response = new RecipeResponse();
        response.setId(recipe.getId());
        response.setTitle(recipe.getTitle());
        response.setDescription(recipe.getDescription());
        response.setPrepTime(recipe.getPrepTime());
        response.setCookTime(recipe.getCookTime());
        response.setTotalTime(recipe.getTotalTime());
        response.setServings(recipe.getServings());
        response.setDifficulty(recipe.getDifficulty().name());
        response.setImageUrl(recipe.getImageUrl());
        response.setIngredients(recipe.getIngredients());
        response.setSteps(recipe.getSteps());
        response.setUserId(recipe.getUser().getId());
        response.setUsername(recipe.getUser().getUsername());
        response.setCreatedAt(recipe.getCreatedAt());
        response.setUpdatedAt(recipe.getUpdatedAt());

        Integer likes = recipe.getLikesCount() != null ? recipe.getLikesCount() : 0;
        Integer views = recipe.getViewsCount() != null ? recipe.getViewsCount() : 0;

        response.setLikesCount(likes);
        response.setViewsCount(views);

        return response;
    }
}