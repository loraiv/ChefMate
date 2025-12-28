package com.chefmate.backend.controller;

import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.service.RecipeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/recipes")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    // Създаване на нова рецепта
    @PostMapping
    public ResponseEntity<RecipeResponse> createRecipe(
            @RequestBody RecipeRequest request,
            @RequestHeader("User-Id") Long userId) {

        RecipeResponse response = recipeService.createRecipe(request, userId);
        return ResponseEntity.ok(response);
    }

    // Взимане на всички рецепти
    @GetMapping
    public ResponseEntity<List<RecipeResponse>> getAllRecipes() {
        List<RecipeResponse> recipes = recipeService.getAllRecipes();
        return ResponseEntity.ok(recipes);
    }

    // Взимане на рецепта по ID
    @GetMapping("/{id}")
    public ResponseEntity<RecipeResponse> getRecipeById(@PathVariable Long id) {
        RecipeResponse recipe = recipeService.getRecipeById(id);
        return ResponseEntity.ok(recipe);
    }

    // Взимане на рецепти на конкретен потребител
    @GetMapping("/user/{userId}")
    public ResponseEntity<List<RecipeResponse>> getUserRecipes(@PathVariable Long userId) {
        List<RecipeResponse> recipes = recipeService.getUserRecipes(userId);
        return ResponseEntity.ok(recipes);
    }

    // Тестов endpoint
    @GetMapping("/test")
    public String test() {
        return "Recipe controller is working!";
    }

    // КОМЕНТИРАЙ тези методи засега - нямаме ги в RecipeService
    /*
    // Качване на снимка за рецепта
    @PostMapping("/{id}/upload-image")
    public ResponseEntity<RecipeResponse> uploadImage(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile file) {

        try {
            RecipeResponse response = recipeService.uploadRecipeImage(id, file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(null);
        }
    }

    // Тест за качване на файл
    @PostMapping("/test-upload")
    public ResponseEntity<String> testUpload(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body("Файлът е празен");
        }

        return ResponseEntity.ok("Файлът е получен: " + file.getOriginalFilename() +
                ", Размер: " + file.getSize() + " байта");
    }

    // Изтриване на снимка
    @DeleteMapping("/{id}/image")
    public ResponseEntity<Void> deleteImage(@PathVariable Long id) {
        try {
            recipeService.deleteRecipeImage(id);
            return ResponseEntity.ok().build();
        } catch (IOException e) {
            return ResponseEntity.status(500).build();
        }
    }
    */
}