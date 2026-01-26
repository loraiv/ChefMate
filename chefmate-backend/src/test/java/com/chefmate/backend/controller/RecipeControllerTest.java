package com.chefmate.backend.controller;

import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.RecipeService;
import com.chefmate.backend.utils.JwtUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = RecipeController.class)
@AutoConfigureMockMvc(addFilters = false)
class RecipeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RecipeService recipeService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private com.chefmate.backend.service.FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private RecipeRequest recipeRequest;
    private RecipeResponse recipeResponse;
    private String validToken;

    @BeforeEach
    void setUp() {
        validToken = "Bearer valid-jwt-token";

        recipeRequest = new RecipeRequest();
        recipeRequest.setTitle("Test Recipe");
        recipeRequest.setDescription("Test Description");
        recipeRequest.setPrepTime(10);
        recipeRequest.setCookTime(20);
        recipeRequest.setServings(4);
        recipeRequest.setDifficulty("EASY");
        recipeRequest.setIngredients(Arrays.asList("Ingredient 1", "Ingredient 2"));
        recipeRequest.setSteps(Arrays.asList("Step 1", "Step 2"));

        recipeResponse = new RecipeResponse();
        recipeResponse.setId(1L);
        recipeResponse.setTitle("Test Recipe");
        recipeResponse.setDescription("Test Description");
        recipeResponse.setPrepTime(10);
        recipeResponse.setCookTime(20);
        recipeResponse.setTotalTime(30);
        recipeResponse.setServings(4);
        recipeResponse.setDifficulty("EASY");
    }

    @Test
    void testCreateRecipe_Success() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        when(recipeService.createRecipe(any(RecipeRequest.class), eq(1L))).thenReturn(recipeResponse);

        // Act & Assert
        mockMvc.perform(post("/api/recipes")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recipeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Recipe"));

        verify(recipeService).createRecipe(any(RecipeRequest.class), eq(1L));
    }

    @Test
    void testCreateRecipe_Unauthorized() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(anyString(), any(JwtService.class))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/recipes")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recipeRequest)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please sign in to continue"));

        verify(recipeService, never()).createRecipe(any(), anyLong());
    }

    @Test
    void testGetRecipeById_Success() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        when(recipeService.getRecipeById(1L, 1L)).thenReturn(recipeResponse);

        // Act & Assert
        mockMvc.perform(get("/api/recipes/1")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.title").value("Test Recipe"));

        verify(recipeService).getRecipeById(1L, 1L);
    }

    @Test
    void testGetAllRecipes_Success() throws Exception {
        // Arrange
        RecipeResponse recipe2 = new RecipeResponse();
        recipe2.setId(2L);
        recipe2.setTitle("Recipe 2");

        List<RecipeResponse> recipes = Arrays.asList(recipeResponse, recipe2);
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        when(recipeService.getAllRecipes(1L)).thenReturn(recipes);

        // Act & Assert
        mockMvc.perform(get("/api/recipes")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2));

        verify(recipeService).getAllRecipes(1L);
    }

    @Test
    void testUpdateRecipe_Success() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        when(recipeService.updateRecipe(eq(1L), any(RecipeRequest.class), eq(1L))).thenReturn(recipeResponse);

        // Act & Assert
        mockMvc.perform(put("/api/recipes/1")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(recipeRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));

        verify(recipeService).updateRecipe(eq(1L), any(RecipeRequest.class), eq(1L));
    }

    @Test
    void testDeleteRecipe_Success() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        doNothing().when(recipeService).deleteRecipe(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/recipes/1")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recipe deleted successfully"));

        verify(recipeService).deleteRecipe(1L, 1L);
    }

    @Test
    void testLikeRecipe_Success() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        doNothing().when(recipeService).likeRecipe(1L, 1L);

        // Act & Assert
        mockMvc.perform(post("/api/recipes/1/like")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recipe liked successfully"));

        verify(recipeService).likeRecipe(1L, 1L);
    }

    @Test
    void testUnlikeRecipe_Success() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        doNothing().when(recipeService).unlikeRecipe(1L, 1L);

        // Act & Assert
        mockMvc.perform(delete("/api/recipes/1/like")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recipe unliked successfully"));

        verify(recipeService).unlikeRecipe(1L, 1L);
    }
}
