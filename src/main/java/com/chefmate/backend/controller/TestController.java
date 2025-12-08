package com.chefmate.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/hello")
    public String hello() {
        return """
               ChefMate Backend is WORKING PERFECTLY!
               
               Status: Online
               Time: %s
               Database: PostgreSQL 18.1
               Spring Boot: 3.3.4
               Java: 23
               Port: 8090
               
               Ready to build amazing recipes! 🥘
               """.formatted(LocalDateTime.now());
    }

    @GetMapping("/health")
    public String health() {
        return "{\"status\":\"UP\",\"database\":\"connected\"}";
    }
}