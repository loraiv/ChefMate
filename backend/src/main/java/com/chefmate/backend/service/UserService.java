package com.chefmate.backend.service;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.entity.PasswordResetToken;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.*;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
public class UserService {

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final RecipeRepository recipeRepository;
    private final RecipeLikeRepository recipeLikeRepository;
    private final CommentRepository commentRepository;
    private final ShoppingListRepository shoppingListRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final SecureRandom secureRandom = new SecureRandom();

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       RecipeRepository recipeRepository,
                       RecipeLikeRepository recipeLikeRepository,
                       CommentRepository commentRepository,
                       ShoppingListRepository shoppingListRepository,
                       PasswordResetTokenRepository passwordResetTokenRepository,
                       EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.recipeRepository = recipeRepository;
        this.recipeLikeRepository = recipeLikeRepository;
        this.commentRepository = commentRepository;
        this.shoppingListRepository = shoppingListRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.emailService = emailService;
    }

    public User registerUser(RegisterRequest request) {
        logger.info("Attempting to register user with username: {}, email: {}", 
                   request.getUsername(), request.getEmail());
        
        String username = (request.getUsername() != null) ? request.getUsername().trim().toLowerCase() : "";
        String email = (request.getEmail() != null) ? request.getEmail().trim().toLowerCase() : "";
        
        if (username.isEmpty()) {
            logger.warn("Registration failed: Username is empty");
            throw new IllegalStateException("Username cannot be empty!");
        }
        
        if (email.isEmpty()) {
            logger.warn("Registration failed: Email is empty");
            throw new IllegalStateException("Email cannot be empty!");
        }
        
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            logger.warn("Registration failed: Password is empty");
            throw new IllegalStateException("Password cannot be empty!");
        }

        boolean usernameExists = userRepository.existsByUsernameIgnoreCase(username);
        boolean emailExists = userRepository.existsByEmailIgnoreCase(email);
        
        if (usernameExists) {
            logger.error("Registration failed: Username '{}' is already taken", username);
            throw new IllegalStateException("Username is already taken!");
        }

        if (emailExists) {
            logger.error("Registration failed: Email '{}' is already in use", email);
            throw new IllegalStateException("Email is already in use!");
        }

        try {
            User user = new User();
            user.setUsername(username);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(request.getPassword()));
            
            String firstName = (request.getFirstName() != null) ? request.getFirstName().trim() : null;
            String lastName = (request.getLastName() != null) ? request.getLastName().trim() : null;
            user.setFirstName((firstName != null && !firstName.isEmpty()) ? firstName : null);
            user.setLastName((lastName != null && !lastName.isEmpty()) ? lastName : null);

            User savedUser = userRepository.save(user);
            logger.info("User registered successfully with ID: {}", savedUser.getId());
            return savedUser;
        } catch (DataIntegrityViolationException e) {
            logger.error("Database integrity violation during registration: {}", e.getMessage(), e);
            if (e.getMessage() != null && e.getMessage().toLowerCase().contains("username")) {
                throw new IllegalStateException("Username is already taken!");
            } else if (e.getMessage() != null && e.getMessage().toLowerCase().contains("email")) {
                throw new IllegalStateException("Email is already in use!");
            } else {
                throw new IllegalStateException("Registration failed: " + e.getMessage());
            }
        } catch (Exception e) {
            logger.error("Unexpected error during registration: {}", e.getMessage(), e);
            throw new IllegalStateException("Registration failed: " + e.getMessage());
        }
    }

    public AuthResponse loginUser(LoginRequest request) {
        String usernameOrEmail = (request.getUsernameOrEmail() != null) 
            ? request.getUsernameOrEmail().trim().toLowerCase() 
            : "";
        String password = request.getPassword();

        if (usernameOrEmail.isEmpty()) {
            throw new RuntimeException("Username or email cannot be empty");
        }

        User user = userRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .orElseGet(() -> {
                    return userRepository.findByEmailIgnoreCase(usernameOrEmail)
                            .orElseThrow(() -> new RuntimeException("Invalid username/email or password"));
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new RuntimeException("Invalid username/email or password");
        }

        org.springframework.security.core.userdetails.UserDetails userDetails = 
            org.springframework.security.core.userdetails.User.builder()
                .username(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_USER")
                .build();
        String token = jwtService.generateToken(userDetails, user.getId());

        AuthResponse response = new AuthResponse();
        response.setToken(token);
        response.setType("Bearer");
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());

        return response;
    }

    public boolean checkUsernameExists(String username) {
        String normalizedUsername = (username != null) ? username.trim().toLowerCase() : "";
        return userRepository.existsByUsernameIgnoreCase(normalizedUsername);
    }

    public boolean checkEmailExists(String email) {
        String normalizedEmail = (email != null) ? email.trim().toLowerCase() : "";
        return userRepository.existsByEmailIgnoreCase(normalizedEmail);
    }

    @Transactional
    public String processForgotPassword(String email) {
        String normalizedEmail = (email != null) ? email.trim().toLowerCase() : "";
        
        if (normalizedEmail.isEmpty()) {
            throw new IllegalStateException("Email cannot be empty!");
        }
        
        User user = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        
        String successMessage = "Ако този имейл адрес съществува в нашата система, ще получите инструкции за възстановяване на паролата.";
        
        if (user == null) {
            logger.info("Forgot password requested for non-existent email: {}", normalizedEmail);
            return successMessage;
        }
        
        passwordResetTokenRepository.deleteByUserId(user.getId());
        
        String token = generateResetToken();
        LocalDateTime expiryDate = LocalDateTime.now().plusHours(24);
        
        PasswordResetToken resetToken = new PasswordResetToken(token, user, expiryDate);
        passwordResetTokenRepository.save(resetToken);
        
        logger.info("Password reset token generated for user: {} (email: {})", user.getUsername(), normalizedEmail);
        
        try {
            emailService.sendPasswordResetEmail(normalizedEmail, token);
            logger.info("Password reset email sent successfully to: {}", normalizedEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", normalizedEmail, e);
        }
        
        return successMessage;
    }
    
    private String generateResetToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
    }
    
    @Transactional
    public void resetPassword(String token, String newPassword) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalStateException("Reset token cannot be empty!");
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalStateException("New password cannot be empty!");
        }
        
        if (newPassword.length() < 6) {
            throw new IllegalStateException("Password must be at least 6 characters long!");
        }
        
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new IllegalStateException("Invalid or expired reset token!"));
        
        if (resetToken.getUsed()) {
            throw new IllegalStateException("This reset token has already been used!");
        }
        
        if (resetToken.isExpired()) {
            throw new IllegalStateException("This reset token has expired!");
        }
        
        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        
        passwordResetTokenRepository.deleteByUserId(user.getId());
        
        logger.info("Password reset successfully for user: {}", user.getUsername());
    }

    @Transactional
    public void changePassword(Long userId, String currentPassword, String newPassword) {
        if (currentPassword == null || currentPassword.trim().isEmpty()) {
            throw new IllegalStateException("Current password cannot be empty!");
        }
        
        if (newPassword == null || newPassword.trim().isEmpty()) {
            throw new IllegalStateException("New password cannot be empty!");
        }
        
        if (newPassword.length() < 6) {
            throw new IllegalStateException("New password must be at least 6 characters long!");
        }
        
        if (currentPassword.equals(newPassword)) {
            throw new IllegalStateException("New password must be different from current password!");
        }
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found!"));
        
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalStateException("Current password is incorrect!");
        }
        
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        logger.info("Password changed successfully for user: {}", user.getUsername());
    }

    @Transactional
    public void updateProfileImageUrl(Long userId, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException("User not found!"));
        
        user.setProfileImageUrl(imageUrl);
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
        
        logger.info("Profile image updated for user ID: {}", userId);
    }

    @Transactional
    public int deleteAllUsers() {
        logger.info("Starting to delete all users and related data...");
        
        long recipeLikesCount = recipeLikeRepository.count();
        recipeLikeRepository.deleteAll();
        logger.info("Deleted {} recipe likes", recipeLikesCount);
        
        long commentsCount = commentRepository.count();
        commentRepository.deleteAll();
        logger.info("Deleted {} comments", commentsCount);
        
        long recipesCount = recipeRepository.count();
        recipeRepository.deleteAll();
        logger.info("Deleted {} recipes", recipesCount);
        
        long shoppingListsCount = shoppingListRepository.count();
        shoppingListRepository.deleteAll();
        logger.info("Deleted {} shopping lists", shoppingListsCount);
        
        long usersCount = userRepository.count();
        userRepository.deleteAll();
        logger.info("Deleted {} users from database", usersCount);
        
        logger.info("Successfully deleted all users and related data");
        return (int) usersCount;
    }
}