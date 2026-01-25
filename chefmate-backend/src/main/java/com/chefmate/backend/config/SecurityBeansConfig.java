package com.chefmate.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class SecurityBeansConfig {

    private static final Logger logger = LoggerFactory.getLogger(SecurityBeansConfig.class);

    @Bean
    public PasswordEncoder passwordEncoder() {
        logger.debug("PasswordEncoder bean created");
        return new BCryptPasswordEncoder();
    }
}
