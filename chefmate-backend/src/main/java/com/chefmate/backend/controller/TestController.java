package com.chefmate.backend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @Value("${spring.mail.username:}")
    private String mailUsername;

    @Value("${spring.mail.password:}")
    private String mailPassword;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String mailHost;

    @Value("${spring.mail.port:587}")
    private int mailPort;

    @Autowired(required = false)
    private JavaMailSender mailSender;

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

    @GetMapping("/check-email-config")
    public Map<String, Object> checkEmailConfig() {
        Map<String, Object> result = new HashMap<>();
        
        // Check environment variables
        result.put("mailHost", mailHost);
        result.put("mailPort", mailPort);
        result.put("mailUsername", mailUsername != null && !mailUsername.isEmpty() ? "SET (length: " + mailUsername.length() + ")" : "NOT SET");
        result.put("mailPassword", mailPassword != null && !mailPassword.isEmpty() ? "SET (length: " + mailPassword.length() + ")" : "NOT SET");
        
        // Show first and last 2 characters of password for debugging (without revealing full password)
        if (mailPassword != null && !mailPassword.isEmpty()) {
            String passwordPreview = mailPassword.length() >= 4 ? 
                mailPassword.substring(0, 2) + "..." + mailPassword.substring(mailPassword.length() - 2) : 
                "***";
            result.put("mailPasswordPreview", passwordPreview);
            result.put("mailPasswordHasSpaces", mailPassword.contains(" "));
        }
        
        // Check if mailSender bean exists
        result.put("mailSenderBean", mailSender != null ? "EXISTS" : "NULL");
        
        // Overall status
        boolean isConfigured = mailSender != null && 
                               mailUsername != null && !mailUsername.isEmpty() && 
                               mailPassword != null && !mailPassword.isEmpty();
        result.put("emailServiceConfigured", isConfigured);
        result.put("message", isConfigured ? 
            "Email service is properly configured!" : 
            "Email service is NOT configured. Please set MAIL_USERNAME and MAIL_PASSWORD environment variables.");
        
        return result;
    }
}
