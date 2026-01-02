package com.chefmate.backend.service;

import com.chefmate.backend.dto.AuthResponse;
import com.chefmate.backend.dto.LoginRequest;
import com.chefmate.backend.dto.RegisterRequest;
import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public User registerUser(RegisterRequest request) {
        String username = request.getUsername();
        String email = request.getEmail();
        
        if (username == null || username.trim().isEmpty()) {
            throw new RuntimeException("Username cannot be empty!");
        }
        
        if (email == null || email.trim().isEmpty()) {
            throw new RuntimeException("Email cannot be empty!");
        }
        
        username = username.trim();
        email = email.trim().toLowerCase();
        
        Optional<User> existingUserByUsername = userRepository.findByUsername(username);
        if (existingUserByUsername.isPresent()) {
            throw new RuntimeException("Username is already taken!");
        }

        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            throw new RuntimeException("Email is already in use!");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());

        return userRepository.save(user);
    }

    public AuthResponse loginUser(LoginRequest request) {
        String usernameOrEmail = request.getUsernameOrEmail();
        String password = request.getPassword();

        User user = userRepository.findByUsername(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmail(usernameOrEmail)
                        .orElseThrow(() -> new RuntimeException("Invalid username/email or password")));

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

    public String generateTokenForUser(org.springframework.security.core.userdetails.UserDetails userDetails, Long userId) {
        return jwtService.generateToken(userDetails, userId);
    }
}