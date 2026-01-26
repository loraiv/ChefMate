package com.chefmate.backend.service;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.ChangePasswordRequest;
import com.chefmate.backend.dto.ForgotPasswordRequest;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.dto.ResetPasswordRequest;
import com.chefmate.backend.entity.PasswordResetToken;
import com.chefmate.backend.entity.Role;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.PasswordResetTokenRepository;
import com.chefmate.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @InjectMocks
    private AuthService authService;

    private User testUser;
    private RegisterRequest registerRequest;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setUsername("testuser");
        testUser.setEmail("test@example.com");
        testUser.setPassword("encodedPassword");
        testUser.setFirstName("Test");
        testUser.setLastName("User");
        testUser.setRole(Role.USER);
        testUser.setEnabled(true);

        registerRequest = new RegisterRequest();
        registerRequest.setUsername("newuser");
        registerRequest.setEmail("newuser@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFirstName("New");
        registerRequest.setLastName("User");

        loginRequest = new LoginRequest();
        loginRequest.setUsernameOrEmail("testuser");
        loginRequest.setPassword("password123");
    }

    @Test
    void testRegister_Success() {
        // Arrange
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encodedPassword");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User user = invocation.getArgument(0);
            user.setId(1L);
            return user;
        });
        when(jwtService.generateToken(any(UserDetails.class), anyLong())).thenReturn("test-token");

        // Act
        AuthResponse response = authService.register(registerRequest);

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("newuser", response.getUsername());
        assertEquals("newuser@example.com", response.getEmail());
        assertEquals("New", response.getFirstName());
        assertEquals("User", response.getLastName());
        assertEquals("USER", response.getRole());

        verify(userRepository).existsByUsernameIgnoreCase("newuser");
        verify(userRepository).existsByEmailIgnoreCase("newuser@example.com");
        verify(userRepository).save(any(User.class));
        verify(jwtService).generateToken(any(UserDetails.class), eq(1L));
    }

    @Test
    void testRegister_UsernameAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("This username is already taken. Please choose a different one.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testRegister_EmailAlreadyExists() {
        // Arrange
        when(userRepository.existsByUsernameIgnoreCase(anyString())).thenReturn(false);
        when(userRepository.existsByEmailIgnoreCase(anyString())).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals("This email is already registered. Please use a different email address or sign in.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testLogin_Success() {
        // Arrange
        Authentication authentication = mock(Authentication.class);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("testuser")
                .password("encodedPassword")
                .authorities("ROLE_USER")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(userDetails);
        when(userRepository.findByUsernameIgnoreCase("testuser")).thenReturn(Optional.of(testUser));
        when(jwtService.generateToken(any(UserDetails.class), anyLong())).thenReturn("test-token");

        // Act
        AuthResponse response = authService.login(loginRequest);

        // Assert
        assertNotNull(response);
        assertEquals("test-token", response.getToken());
        assertEquals(1L, response.getId());
        assertEquals("testuser", response.getUsername());
        assertEquals("test@example.com", response.getEmail());

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService).generateToken(any(UserDetails.class), eq(1L));
    }

    @Test
    void testLogin_InvalidCredentials() {
        // Arrange
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Invalid credentials"));

        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> {
            authService.login(loginRequest);
        });

        verify(userRepository, never()).findByUsernameIgnoreCase(anyString());
        verify(jwtService, never()).generateToken(any(), anyLong());
    }

    @Test
    void testChangePassword_Success() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("newPassword123", "encodedPassword")).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");

        // Act
        java.util.Map<String, String> response = authService.changePassword(1L, request);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("message"));
        assertEquals("Your password has been changed successfully", response.get("message"));

        verify(userRepository).findById(1L);
        verify(passwordEncoder).matches("oldPassword", "encodedPassword");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(testUser);
    }

    @Test
    void testChangePassword_InvalidCurrentPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPassword123");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPassword", "encodedPassword")).thenReturn(false);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword(1L, request);
        });

        assertEquals("Current password is incorrect. Please try again.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_NewPasswordTooShort() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("short");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        // Note: passwordEncoder.matches is not called because validation happens before password check

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword(1L, request);
        });

        assertEquals("New password must be at least 6 characters long", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testChangePassword_NewPasswordSameAsCurrent() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("oldPassword");
        request.setNewPassword("oldPassword");

        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);
        when(passwordEncoder.matches("oldPassword", "encodedPassword")).thenReturn(true);

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.changePassword(1L, request);
        });

        assertEquals("New password must be different from your current password", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testForgotPassword_UserExists() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(emailService.sendPasswordResetEmail(anyString(), anyString())).thenReturn(true);

        // Act
        java.util.Map<String, String> response = authService.forgotPassword(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("message"));
        assertTrue(response.get("message").contains("Password reset instructions have been sent"));

        verify(userRepository).findByEmailIgnoreCase("test@example.com");
        verify(passwordResetTokenRepository).deleteByUserId(1L);
        verify(passwordResetTokenRepository).save(any(PasswordResetToken.class));
        verify(emailService).sendPasswordResetEmail(eq("test@example.com"), anyString());
    }

    @Test
    void testForgotPassword_UserNotExists() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent@example.com");

        when(userRepository.findByEmailIgnoreCase("nonexistent@example.com")).thenReturn(Optional.empty());

        // Act
        java.util.Map<String, String> response = authService.forgotPassword(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("message"));
        assertTrue(response.get("message").contains("If the email exists"));

        verify(userRepository).findByEmailIgnoreCase("nonexistent@example.com");
        verify(passwordResetTokenRepository, never()).save(any(PasswordResetToken.class));
        verify(emailService, never()).sendPasswordResetEmail(anyString(), anyString());
    }

    @Test
    void testResetPassword_Success() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("newPassword123");

        PasswordResetToken resetToken = new PasswordResetToken("valid-token", testUser, LocalDateTime.now().plusHours(1));
        resetToken.setUsed(false);

        when(passwordResetTokenRepository.findByToken("valid-token")).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("newEncodedPassword");
        when(passwordResetTokenRepository.save(any(PasswordResetToken.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        java.util.Map<String, String> response = authService.resetPassword(request);

        // Assert
        assertNotNull(response);
        assertTrue(response.containsKey("message"));
        assertTrue(response.get("message").contains("password has been reset successfully"));

        verify(passwordResetTokenRepository).findByToken("valid-token");
        verify(passwordEncoder).encode("newPassword123");
        verify(userRepository).save(testUser);
        verify(passwordResetTokenRepository).save(resetToken);
        assertTrue(resetToken.getUsed());
    }

    @Test
    void testResetPassword_InvalidToken() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");

        when(passwordResetTokenRepository.findByToken("invalid-token")).thenReturn(Optional.empty());

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("Invalid or expired reset token", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testResetPassword_ExpiredToken() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("expired-token");
        request.setNewPassword("newPassword123");

        PasswordResetToken resetToken = new PasswordResetToken("expired-token", testUser, LocalDateTime.now().minusHours(2));
        resetToken.setUsed(false);

        when(passwordResetTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(resetToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("This reset link has expired. Please request a new password reset.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testResetPassword_UsedToken() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("used-token");
        request.setNewPassword("newPassword123");

        PasswordResetToken resetToken = new PasswordResetToken("used-token", testUser, LocalDateTime.now().plusHours(1));
        resetToken.setUsed(true);

        when(passwordResetTokenRepository.findByToken("used-token")).thenReturn(Optional.of(resetToken));

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("This reset link has already been used. Please request a new password reset.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void testResetPassword_PasswordTooShort() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("valid-token");
        request.setNewPassword("short");

        // Act & Assert
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            authService.resetPassword(request);
        });

        assertEquals("Password must be at least 6 characters long", exception.getMessage());
        verify(passwordResetTokenRepository, never()).findByToken(anyString());
    }
}
