package com.chefmate.backend.controller;

import com.chefmate.backend.dto.UserManagementResponse;
import com.chefmate.backend.entity.Role;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.CommentRepository;
import com.chefmate.backend.repository.RecipeRepository;
import com.chefmate.backend.repository.UserRepository;
import com.chefmate.backend.service.JwtService;
import com.chefmate.backend.service.UserService;
import com.chefmate.backend.service.RecipeService;
import com.chefmate.backend.utils.JwtUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private static final Logger logger = LoggerFactory.getLogger(AdminController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserService userService;
    private final RecipeService recipeService;
    private final RecipeRepository recipeRepository;
    private final CommentRepository commentRepository;

    public AdminController(UserRepository userRepository, 
                          PasswordEncoder passwordEncoder,
                          JwtService jwtService,
                          UserService userService,
                          RecipeRepository recipeRepository,
                          CommentRepository commentRepository,
                          RecipeService recipeService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.userService = userService;
        this.recipeRepository = recipeRepository;
        this.commentRepository = commentRepository;
        this.recipeService = recipeService;
    }

    /**
     * Helper method to check if current user is admin
     */
    private User checkAdminAuth(String token) {
        Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (currentUserId == null) {
            throw new RuntimeException("Unauthorized");
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (currentUser.getRole() != Role.ADMIN) {
            throw new RuntimeException("Only admins can perform this action");
        }
        
        return currentUser;
    }

    /**
     * Helper method to convert User to UserManagementResponse
     */
    private UserManagementResponse toUserManagementResponse(User user) {
        UserManagementResponse response = new UserManagementResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setFirstName(user.getFirstName());
        response.setLastName(user.getLastName());
        response.setRole(user.getRole().name());
        response.setEnabled(user.getEnabled());
        response.setProfileImageUrl(user.getProfileImageUrl());
        response.setCreatedAt(user.getCreatedAt());
        response.setUpdatedAt(user.getUpdatedAt());
        return response;
    }

    /**
     * Check if a user exists and get basic info (for debugging)
     */
    @GetMapping("/check-user")
    public ResponseEntity<?> checkUser(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String email) {
        try {
            User user = null;
            if (username != null) {
                user = userRepository.findByUsernameIgnoreCase(username).orElse(null);
            } else if (email != null) {
                user = userRepository.findByEmailIgnoreCase(email).orElse(null);
            }
            
            if (user == null) {
                Map<String, Object> response = new HashMap<>();
                response.put("exists", false);
                response.put("message", "User not found");
                return ResponseEntity.ok(response);
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("exists", true);
            response.put("id", user.getId());
            response.put("username", user.getUsername());
            response.put("email", user.getEmail());
            response.put("role", user.getRole().name());
            response.put("enabled", user.getEnabled());
            response.put("hasPassword", user.getPassword() != null && !user.getPassword().isEmpty());
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Creates an admin account (only for initial setup)
     * WARNING: This should be disabled in production after creating the first admin!
     */
    @PostMapping("/create-admin")
    public ResponseEntity<Map<String, Object>> createAdmin(
            @RequestParam String username,
            @RequestParam String email,
            @RequestParam String password) {
        
        if (userRepository.existsByUsernameIgnoreCase(username)) {
            Map<String, Object> response = new HashMap<>();
                response.put("error", "This username is already taken. Please choose a different one.");
            return ResponseEntity.badRequest().body(response);
        }
        
        if (userRepository.existsByEmailIgnoreCase(email)) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "This email is already registered. Please use a different email address.");
            return ResponseEntity.badRequest().body(response);
        }

        User admin = new User();
        admin.setUsername(username);
        admin.setEmail(email);
        admin.setPassword(passwordEncoder.encode(password));
        admin.setRole(Role.ADMIN);
        
        User savedAdmin = userRepository.save(admin);
        logger.info("Admin account created: {}", savedAdmin.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin account created successfully");
        response.put("username", savedAdmin.getUsername());
        response.put("id", savedAdmin.getId());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Promotes a user to admin role (requires admin authentication)
     */
    @PostMapping("/promote-to-admin/{userId}")
    public ResponseEntity<Map<String, Object>> promoteToAdmin(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (currentUserId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Please sign in to continue");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (currentUser.getRole() != Role.ADMIN) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Only administrators can promote users to admin role");
            return ResponseEntity.status(403).body(response);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        user.setRole(Role.ADMIN);
        userRepository.save(user);
        logger.info("User {} promoted to admin by {}", user.getUsername(), currentUser.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "User promoted to admin successfully");
        response.put("username", user.getUsername());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Demotes an admin to user role (requires admin authentication)
     */
    @PostMapping("/demote-from-admin/{userId}")
    public ResponseEntity<Map<String, Object>> demoteFromAdmin(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        
        Long currentUserId = JwtUtils.getUserIdFromToken(token, jwtService);
        if (currentUserId == null) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Please sign in to continue");
            return ResponseEntity.status(401).body(response);
        }

        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (currentUser.getRole() != Role.ADMIN) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "Only administrators can demote admin users");
            return ResponseEntity.status(403).body(response);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        if (user.getRole() != Role.ADMIN) {
            Map<String, Object> response = new HashMap<>();
            response.put("error", "This user is not an administrator");
            return ResponseEntity.badRequest().body(response);
        }

        user.setRole(Role.USER);
        userRepository.save(user);
        logger.info("Admin {} demoted to user by {}", user.getUsername(), currentUser.getUsername());
        
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Admin demoted to user successfully");
        response.put("username", user.getUsername());
        
        return ResponseEntity.ok(response);
    }

    // ========== USER MANAGEMENT ==========

    /**
     * Get all users (admin only)
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers(
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            checkAdminAuth(token);
            
            List<User> users = userRepository.findAll();
            List<UserManagementResponse> response = users.stream()
                    .map(this::toUserManagementResponse)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            int status = e.getMessage().contains("Unauthorized") ? 401 : 403;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * Get user by ID (admin only)
     */
    @GetMapping("/users/{userId}")
    public ResponseEntity<?> getUserById(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            checkAdminAuth(token);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            return ResponseEntity.ok(toUserManagementResponse(user));
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            int status = e.getMessage().contains("Unauthorized") ? 401 : 
                        e.getMessage().contains("not found") ? 404 : 403;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * Block/disable a user (admin only)
     */
    @PostMapping("/users/{userId}/block")
    public ResponseEntity<Map<String, Object>> blockUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            User currentUser = checkAdminAuth(token);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getRole() == Role.ADMIN && !user.getId().equals(currentUser.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Administrators cannot block other administrators");
                return ResponseEntity.status(403).body(response);
            }
            
            user.setEnabled(false);
            userRepository.save(user);
            logger.info("User {} blocked by {}", user.getUsername(), currentUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User blocked successfully");
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            int status = e.getMessage().contains("Unauthorized") ? 401 : 403;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * Unblock/enable a user (admin only)
     */
    @PostMapping("/users/{userId}/unblock")
    public ResponseEntity<Map<String, Object>> unblockUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            User currentUser = checkAdminAuth(token);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            user.setEnabled(true);
            userRepository.save(user);
            logger.info("User {} unblocked by {}", user.getUsername(), currentUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User unblocked successfully");
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            int status = e.getMessage().contains("Unauthorized") ? 401 : 403;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * Delete a user account (admin only)
     */
    @DeleteMapping("/users/{userId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteUser(
            @PathVariable Long userId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            User currentUser = checkAdminAuth(token);
            
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            if (user.getRole() == Role.ADMIN && !user.getId().equals(currentUser.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "Administrators cannot delete other administrators");
                return ResponseEntity.status(403).body(response);
            }
            
            if (user.getId().equals(currentUser.getId())) {
                Map<String, Object> response = new HashMap<>();
                response.put("error", "You cannot delete your own account");
                return ResponseEntity.status(400).body(response);
            }
            
            // Use UserService to delete user (handles cascading deletes)
            userService.deleteAccount(userId);
            logger.info("User {} deleted by {}", user.getUsername(), currentUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "User deleted successfully");
            response.put("username", user.getUsername());
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            int status = e.getMessage().contains("Unauthorized") ? 401 : 403;
            return ResponseEntity.status(status).body(error);
        }
    }

    // ========== CONTENT MODERATION ==========

    /**
     * Delete a recipe (admin only)
     */
    @DeleteMapping("/recipes/{recipeId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteRecipe(
            @PathVariable Long recipeId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            User currentUser = checkAdminAuth(token);
            
            var recipe = recipeRepository.findById(recipeId)
                    .orElseThrow(() -> new RuntimeException("Recipe not found"));
            
            String recipeTitle = recipe.getTitle();
            recipeRepository.delete(recipe);
            logger.info("Recipe {} (ID: {}) deleted by admin {}", recipeTitle, recipeId, currentUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Recipe deleted successfully");
            response.put("recipeId", recipeId);
            response.put("recipeTitle", recipeTitle);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            int status = e.getMessage().contains("Unauthorized") ? 401 : 
                        e.getMessage().contains("not found") ? 404 : 403;
            return ResponseEntity.status(status).body(error);
        }
    }

    /**
     * Delete a comment (admin only)
     */
    @DeleteMapping("/comments/{commentId}")
    @Transactional
    public ResponseEntity<Map<String, Object>> deleteComment(
            @PathVariable Long commentId,
            @RequestHeader(value = "Authorization", required = false) String token) {
        try {
            User currentUser = checkAdminAuth(token);
            
            // Use RecipeService to delete the comment along with all its replies and likes
            recipeService.deleteCommentAsAdmin(commentId);
            logger.info("Comment {} deleted by admin {}", commentId, currentUser.getUsername());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Comment deleted successfully");
            response.put("commentId", commentId);
            
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            int status = e.getMessage().contains("Unauthorized") ? 401 : 
                        e.getMessage().contains("not found") ? 404 : 403;
            return ResponseEntity.status(status).body(error);
        }
    }
}
