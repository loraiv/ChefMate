package com.chefmate.backend.service;

import com.chefmate.backend.dto.RecipeRequest;
import com.chefmate.backend.dto.RecipeResponse;
import com.chefmate.backend.entity.Recipe;
import com.chefmate.backend.entity.Role;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.CommentLikeRepository;
import com.chefmate.backend.repository.CommentRepository;
import com.chefmate.backend.repository.RecipeLikeRepository;
import com.chefmate.backend.repository.RecipeRepository;
import com.chefmate.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RecipeServiceTest {

    @Mock
    private RecipeRepository recipeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecipeLikeRepository recipeLikeRepository;

    @Mock
    private CommentRepository commentRepository;

    @Mock
    private CommentLikeRepository commentLikeRepository;

    @InjectMocks
    private RecipeService recipeService;

    private User testUser;
    private Recipe testRecipe;
    private RecipeRequest recipeRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);

        testRecipe = new Recipe();
        testRecipe.setId(1L);
        testRecipe.setTitle("Test Recipe");
        testRecipe.setDescription("Test Description");
        testRecipe.setPrepTime(10);
        testRecipe.setCookTime(20);
        testRecipe.setTotalTime(30);
        testRecipe.setServings(4);
        testRecipe.setDifficulty(Recipe.Difficulty.EASY);
        testRecipe.setUser(testUser);
        testRecipe.setIngredients(Arrays.asList("Ingredient 1", "Ingredient 2"));
        testRecipe.setSteps(Arrays.asList("Step 1", "Step 2"));
        testRecipe.setLikesCount(0);
        testRecipe.setViewsCount(0);

        recipeRequest = new RecipeRequest();
        recipeRequest.setTitle("New Recipe");
        recipeRequest.setDescription("New Description");
        recipeRequest.setPrepTime(15);
        recipeRequest.setCookTime(25);
        recipeRequest.setServings(6);
        recipeRequest.setDifficulty("MEDIUM");
        recipeRequest.setIngredients(Arrays.asList("New Ingredient 1", "New Ingredient 2"));
        recipeRequest.setSteps(Arrays.asList("New Step 1", "New Step 2"));
    }

    @Test
    void testCreateRecipe_Success() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe recipe = invocation.getArgument(0);
            recipe.setId(1L);
            return recipe;
        });
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        RecipeResponse response = recipeService.createRecipe(recipeRequest, 1L);

        // Assert
        assertNotNull(response);
        assertEquals("New Recipe", response.getTitle());
        assertEquals("New Description", response.getDescription());
        assertEquals(15, response.getPrepTime());
        assertEquals(25, response.getCookTime());
        assertEquals(40, response.getTotalTime()); // prepTime + cookTime
        assertEquals(6, response.getServings());
        assertEquals("MEDIUM", response.getDifficulty());
        assertEquals(2, response.getIngredients().size());
        assertEquals(2, response.getSteps().size());

        verify(userRepository).findById(1L);
        verify(recipeRepository).save(any(Recipe.class));
    }

    @Test
    void testCreateRecipe_UserNotFound() {
        // Arrange
        when(userRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            recipeService.createRecipe(recipeRequest, 1L);
        });

        assertEquals("User not found with ID: 1", exception.getMessage());
        verify(recipeRepository, never()).save(any(Recipe.class));
    }

    @Test
    void testGetRecipeById_Success() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        RecipeResponse response = recipeService.getRecipeById(1L, 1L);

        // Assert
        assertNotNull(response);
        assertEquals(1L, response.getId());
        assertEquals("Test Recipe", response.getTitle());
        assertEquals(1, testRecipe.getViewsCount()); // Views should be incremented

        verify(recipeRepository).findById(1L);
        verify(recipeRepository).save(testRecipe);
    }

    @Test
    void testGetRecipeById_NotFound() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            recipeService.getRecipeById(1L, 1L);
        });

        assertEquals("Recipe not found with ID: 1", exception.getMessage());
    }

    @Test
    void testUpdateRecipe_Success() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        RecipeResponse response = recipeService.updateRecipe(1L, recipeRequest, 1L);

        // Assert
        assertNotNull(response);
        assertEquals("New Recipe", response.getTitle());
        assertEquals("New Description", response.getDescription());
        assertEquals(40, response.getTotalTime());

        verify(recipeRepository).findById(1L);
        verify(recipeRepository).save(testRecipe);
    }

    @Test
    void testUpdateRecipe_NotFound() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            recipeService.updateRecipe(1L, recipeRequest, 1L);
        });

        assertEquals("Recipe not found with ID: 1", exception.getMessage());
    }

    @Test
    void testUpdateRecipe_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        testRecipe.setUser(otherUser);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            recipeService.updateRecipe(1L, recipeRequest, 1L);
        });

        assertEquals("You are not authorized to update this recipe", exception.getMessage());
        verify(recipeRepository, never()).save(any(Recipe.class));
    }

    @Test
    void testDeleteRecipe_Success() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        doNothing().when(recipeLikeRepository).deleteByRecipeId(anyLong());
        doNothing().when(commentRepository).deleteByRecipeId(anyLong());
        doNothing().when(recipeRepository).delete(any(Recipe.class));

        // Act
        recipeService.deleteRecipe(1L, 1L);

        // Assert
        verify(recipeRepository).findById(1L);
        verify(recipeLikeRepository).deleteByRecipeId(1L);
        verify(commentRepository).deleteByRecipeId(1L);
        verify(recipeRepository).delete(testRecipe);
    }

    @Test
    void testDeleteRecipe_Unauthorized() {
        // Arrange
        User otherUser = new User();
        otherUser.setId(2L);
        testRecipe.setUser(otherUser);

        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            recipeService.deleteRecipe(1L, 1L);
        });

        assertEquals("You are not authorized to delete this recipe", exception.getMessage());
        verify(recipeRepository, never()).delete(any(Recipe.class));
    }

    @Test
    void testLikeRecipe_Success() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeLikeRepository.existsByRecipeIdAndUserId(1L, 1L)).thenReturn(false);
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        when(recipeLikeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        recipeService.likeRecipe(1L, 1L);

        // Assert
        verify(recipeRepository).findById(1L);
        verify(userRepository).findById(1L);
        verify(recipeLikeRepository).existsByRecipeIdAndUserId(1L, 1L);
        verify(recipeLikeRepository).save(any());
        verify(recipeRepository).save(testRecipe);
        assertEquals(1, testRecipe.getLikesCount());
    }

    @Test
    void testLikeRecipe_AlreadyLiked() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeLikeRepository.existsByRecipeIdAndUserId(1L, 1L)).thenReturn(true);

        // Act
        recipeService.likeRecipe(1L, 1L);

        // Assert
        verify(recipeLikeRepository, never()).save(any());
        verify(recipeRepository, never()).save(any(Recipe.class));
    }

    @Test
    void testUnlikeRecipe_Success() {
        // Arrange
        when(recipeRepository.findById(1L)).thenReturn(Optional.of(testRecipe));
        testRecipe.setLikesCount(1);
        
        // Create a real RecipeLike instance instead of mocking
        com.chefmate.backend.entity.RecipeLike like = new com.chefmate.backend.entity.RecipeLike(testRecipe, testUser);
        when(recipeLikeRepository.findByRecipeIdAndUserId(1L, 1L)).thenReturn(Optional.of(like));
        when(recipeRepository.save(any(Recipe.class))).thenReturn(testRecipe);
        doNothing().when(recipeLikeRepository).delete(any());

        // Act
        recipeService.unlikeRecipe(1L, 1L);

        // Assert
        verify(recipeLikeRepository).findByRecipeIdAndUserId(1L, 1L);
        verify(recipeLikeRepository).delete(like);
        verify(recipeRepository).save(testRecipe);
        assertEquals(0, testRecipe.getLikesCount());
    }

    @Test
    void testGetAllRecipes_Success() {
        // Arrange
        Recipe recipe2 = new Recipe();
        recipe2.setId(2L);
        recipe2.setTitle("Recipe 2");
        recipe2.setUser(testUser);
        recipe2.setLikesCount(0);
        recipe2.setViewsCount(0);

        List<Recipe> recipes = Arrays.asList(testRecipe, recipe2);
        when(recipeRepository.findAll()).thenReturn(recipes);
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        List<RecipeResponse> responses = recipeService.getAllRecipes(1L);

        // Assert
        assertNotNull(responses);
        assertEquals(2, responses.size());
        assertEquals("Test Recipe", responses.get(0).getTitle());
        assertEquals("Recipe 2", responses.get(1).getTitle());

        verify(recipeRepository).findAll();
    }

    @Test
    void testGetUserRecipes_Success() {
        // Arrange
        List<Recipe> recipes = Arrays.asList(testRecipe);
        when(recipeRepository.findByUserId(1L)).thenReturn(recipes);
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        List<RecipeResponse> responses = recipeService.getUserRecipes(1L, 1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Test Recipe", responses.get(0).getTitle());

        verify(recipeRepository).findByUserId(1L);
    }

    @Test
    void testSearchRecipes_ByTitle() {
        // Arrange
        Recipe recipe2 = new Recipe();
        recipe2.setId(2L);
        recipe2.setTitle("Pasta Recipe");
        recipe2.setDescription("Delicious pasta");
        recipe2.setUser(testUser);
        recipe2.setLikesCount(0);
        recipe2.setViewsCount(0);

        List<Recipe> recipes = Arrays.asList(testRecipe, recipe2);
        when(recipeRepository.findAll()).thenReturn(recipes);
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        List<RecipeResponse> responses = recipeService.searchRecipes("Pasta", null, null, 1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("Pasta Recipe", responses.get(0).getTitle());

        verify(recipeRepository).findAll();
    }

    @Test
    void testSearchRecipes_ByDifficulty() {
        // Arrange
        Recipe recipe2 = new Recipe();
        recipe2.setId(2L);
        recipe2.setTitle("Hard Recipe");
        recipe2.setDifficulty(Recipe.Difficulty.HARD);
        recipe2.setUser(testUser);
        recipe2.setLikesCount(0);
        recipe2.setViewsCount(0);

        List<Recipe> recipes = Arrays.asList(testRecipe, recipe2);
        when(recipeRepository.findAll()).thenReturn(recipes);
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        List<RecipeResponse> responses = recipeService.searchRecipes(null, "HARD", null, 1L);

        // Assert
        assertNotNull(responses);
        assertEquals(1, responses.size());
        assertEquals("HARD", responses.get(0).getDifficulty());

        verify(recipeRepository).findAll();
    }

    @Test
    void testCreateRecipe_WithNullIngredients() {
        // Arrange
        recipeRequest.setIngredients(null);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(recipeRepository.save(any(Recipe.class))).thenAnswer(invocation -> {
            Recipe recipe = invocation.getArgument(0);
            recipe.setId(1L);
            return recipe;
        });
        when(recipeLikeRepository.existsByRecipeIdAndUserId(anyLong(), anyLong())).thenReturn(false);

        // Act
        RecipeResponse response = recipeService.createRecipe(recipeRequest, 1L);

        // Assert
        assertNotNull(response);
        assertNotNull(response.getIngredients());
        assertTrue(response.getIngredients().isEmpty());

        verify(recipeRepository).save(any(Recipe.class));
    }
}
