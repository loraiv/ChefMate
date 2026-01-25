package com.chefmate.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${app.base-url:http://localhost:8090}")
    private String baseUrl;

    public boolean sendPasswordResetEmail(String toEmail, String resetToken) {
        if (mailSender == null) {
            logger.error("Email service is not configured. JavaMailSender is null. Check if MAIL_USERNAME and MAIL_PASSWORD are set.");
            throw new RuntimeException("Email service is not configured. JavaMailSender bean is missing.");
        }
        
        if (fromEmail == null || fromEmail.isEmpty()) {
            logger.error("Email service is not configured. MAIL_USERNAME is not set or is empty. Current value: '{}'", fromEmail);
            throw new RuntimeException("Email service is not configured. MAIL_USERNAME is not set.");
        }
        
        logger.info("Attempting to send email from: {} to: {}", fromEmail, toEmail);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(fromEmail);
            helper.setTo(toEmail);
            helper.setSubject("ChefMate - Password Reset Request");
            
            // Create web link that works in browsers and redirects to app
            String webLink = baseUrl + "/api/auth/reset-password?token=" + resetToken;
            // Deep link for direct app opening
            String deepLink = "chefmate://reset-password?token=" + resetToken;
            
            // HTML email with clickable button/link
            String htmlContent = "<!DOCTYPE html>" +
                "<html>" +
                "<head><meta charset='UTF-8'></head>" +
                "<body style='font-family: Arial, sans-serif; line-height: 1.6; color: #333;'>" +
                "<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>" +
                "<h2 style='color: #7C3AED;'>ChefMate - Password Reset</h2>" +
                "<p>Hello,</p>" +
                "<p>You have requested to reset your password for your ChefMate account.</p>" +
                "<p style='margin: 30px 0;'>" +
                "<a href='" + webLink + "' " +
                "style='display: inline-block; padding: 12px 30px; background-color: #7C3AED; color: #ffffff; text-decoration: none; border-radius: 5px; font-weight: bold;'>" +
                "Reset Password" +
                "</a>" +
                "</p>" +
                "<p>Or copy and paste the following link into your browser:</p>" +
                "<p style='word-break: break-all; color: #7C3AED;'>" + webLink + "</p>" +
                "<p style='margin-top: 30px; font-size: 12px; color: #666;'>" +
                "This link will expire in 1 hour.<br>" +
                "If you did not request a password reset, please ignore this email." +
                "</p>" +
                "<p style='margin-top: 20px;'>" +
                "Best regards,<br>" +
                "<strong>The ChefMate Team</strong>" +
                "</p>" +
                "</div>" +
                "</body>" +
                "</html>";
            
            // Plain text fallback
            String textContent = "Hello,\n\n" +
                "You have requested to reset your password for your ChefMate account.\n\n" +
                "Click on the link below to reset your password:\n" +
                webLink + "\n\n" +
                "This link will expire in 1 hour.\n\n" +
                "If you did not request a password reset, please ignore this email.\n\n" +
                "Best regards,\n" +
                "The ChefMate Team";
            
            helper.setText(textContent, htmlContent);
            
            mailSender.send(message);
            logger.info("Password reset email sent successfully to: {}", toEmail);
            return true;
        } catch (org.springframework.mail.MailAuthenticationException e) {
            logger.error("Email authentication failed. Check if MAIL_PASSWORD (App Password) is correct. Error: {}", e.getMessage());
            throw new RuntimeException("Email authentication failed. Please check your email configuration.", e);
        } catch (org.springframework.mail.MailException e) {
            logger.error("Failed to send password reset email to: {}. Error: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Failed to send email: " + e.getMessage(), e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending password reset email to: {}. Error: {}", toEmail, e.getMessage(), e);
            throw new RuntimeException("Unexpected error while sending email: " + e.getMessage(), e);
        }
    }
}
