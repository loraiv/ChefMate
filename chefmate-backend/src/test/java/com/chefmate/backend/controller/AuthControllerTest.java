package com.chefmate.backend.controller;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.ChangePasswordRequest;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.entity.Role;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.UserRepository;
import com.chefmate.backend.service.AuthService;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.FileStorageService;
import com.chefmate.backend.service.UserService;
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

import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserService userService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private FileStorageService fileStorageService;

    @Autowired
    private ObjectMapper objectMapper;

    private User testUser;
    private AuthResponse authResponse;
    private String validToken;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setRole(Role.USER);

        authResponse = new AuthResponse();
        authResponse.setToken("test-token");
        authResponse.setId(1L);
        authResponse.setUsername("testuser");
        authResponse.setEmail("test@example.com");
        authResponse.setRole("USER");

        validToken = "Bearer valid-jwt-token";
    }

    @Test
    void testRegister_Success() throws Exception {
        // Arrange
        RegisterRequest request = new RegisterRequest();
        request.setUsername("newuser");
        request.setEmail("newuser@example.com");
        request.setPassword("password123");
        request.setFirstName("New");
        request.setLastName("User");

        when(authService.register(any(RegisterRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(authService).register(any(RegisterRequest.class));
    }

    @Test
    void testLogin_Success() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("password123");

        when(authService.login(any(LoginRequest.class))).thenReturn(authResponse);

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("test-token"))
                .andExpect(jsonPath("$.username").value("testuser"));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void testLogin_InvalidCredentials() throws Exception {
        // Arrange
        LoginRequest request = new LoginRequest();
        request.setUsernameOrEmail("testuser");
        request.setPassword("wrongpassword");

        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new org.springframework.security.core.AuthenticationException("Invalid credentials") {});

        // Act & Assert
        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Invalid email or password. Please check your credentials and try again."));

        verify(authService).login(any(LoginRequest.class));
    }

    @Test
    void testChangePassword_Success() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        java.util.Map<String, String> response = new java.util.HashMap<>();
        response.put("message", "Your password has been changed successfully");

        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        when(authService.changePassword(eq(1L), any(ChangePasswordRequest.class))).thenReturn(response);

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", validToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Your password has been changed successfully"));

        verify(authService).changePassword(eq(1L), any(ChangePasswordRequest.class));
    }

    @Test
    void testChangePassword_Unauthorized() throws Exception {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(JwtUtils.getUserIdFromToken(anyString(), any(JwtService.class))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(post("/api/auth/change-password")
                .header("Authorization", "Bearer invalid-token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please sign in to continue"));

        verify(authService, never()).changePassword(anyLong(), any(ChangePasswordRequest.class));
    }

    @Test
    void testGetCurrentUser_Success() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(validToken, jwtService)).thenReturn(1L);
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));

        // Act & Assert
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", validToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"));

        verify(userRepository).findById(1L);
    }

    @Test
    void testGetCurrentUser_Unauthorized() throws Exception {
        // Arrange
        when(JwtUtils.getUserIdFromToken(anyString(), any(JwtService.class))).thenReturn(null);

        // Act & Assert
        mockMvc.perform(get("/api/auth/me")
                .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("Please sign in to continue"));

        verify(userRepository, never()).findById(anyLong());
    }
}
