package com.chefmate.backend.service;

import com.chefmate.backend.entity.User;
import com.chefmate.backend.repository.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String usernameOrEmail) throws UsernameNotFoundException {
        User user = userRepository.findByUsernameIgnoreCase(usernameOrEmail)
                .orElseGet(() -> userRepository.findByEmailIgnoreCase(usernameOrEmail)
                        .orElseThrow(() -> new UsernameNotFoundException(
                                "User not found: " + usernameOrEmail)));

        // Handle null enabled field (for existing users before this field was added)
        boolean isEnabled = user.getEnabled() == null || user.getEnabled();
        
        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPassword())
                .authorities("ROLE_" + user.getRole().name())
                .disabled(!isEnabled)
                .build();
    }
}