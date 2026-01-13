package com.chefmate.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);
    
    private final JavaMailSender mailSender;
    
    @Value("${app.base-url:http://localhost:8090}")
    private String baseUrl;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendPasswordResetEmail(String toEmail, String resetToken) {
        try {
            String resetUrl = baseUrl + "/api/auth/reset-password?token=" + resetToken;
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail.isEmpty() ? "noreply@chefmate.com" : fromEmail);
            message.setTo(toEmail);
            message.setSubject("Възстановяване на парола - ChefMate");
            message.setText(
                "Здравейте,\n\n" +
                "Получихме заявка за възстановяване на паролата на вашия акаунт.\n\n" +
                "За да възстановите паролата си, моля кликнете на следния линк:\n" +
                resetUrl + "\n\n" +
                "Този линк е валиден за 24 часа.\n\n" +
                "Ако не сте поискали възстановяване на парола, моля игнорирайте този имейл.\n\n" +
                "Поздрави,\n" +
                "Екипът на ChefMate"
            );
            
            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);
        } catch (Exception e) {
            logger.error("Failed to send password reset email to: {}", toEmail, e);
            throw new RuntimeException("Failed to send email: " + e.getMessage());
        }
    }
}

