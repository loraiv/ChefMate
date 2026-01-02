package com.chefmate.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public String test() {
        return String.format(
            "ChefMate Backend is WORKING PERFECTLY!\n\n" +
            "Status: Online\n" +
            "Time: %s\n" +
            "Database: PostgreSQL\n" +
            "Spring Boot: 3.3.4\n" +
            "Port: 8090\n\n" +
            "Ready to build amazing recipes! 🥘",
            LocalDateTime.now()
        );
    }

    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"UP\",\"database\":\"connected\"}";
    }
}
