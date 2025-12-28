package com.chefmate.backend.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class TestController {

    @GetMapping("/test")
    public String test() {
        return "ChefMate Backend is working!";
    }

    @GetMapping("/health")
    public String health() {
        return "{\"status\": \"UP\", \"service\": \"ChefMate Backend\"}";
    }
}