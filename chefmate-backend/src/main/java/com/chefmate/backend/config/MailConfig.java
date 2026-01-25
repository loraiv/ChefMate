package com.chefmate.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Properties;

@Configuration
public class MailConfig {

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String host;

    @Value("${spring.mail.port:587}")
    private int port;

    @Value("${spring.mail.username:}")
    private String username;

    @Value("${spring.mail.password:}")
    private String password;

    @Bean
    public JavaMailSender javaMailSender() {
        // Check if email is configured
        if (username == null || username.isEmpty() || password == null || password.isEmpty()) {
            System.out.println("WARNING: Email service is not configured. MAIL_USERNAME and/or MAIL_PASSWORD are not set.");
            System.out.println("MAIL_USERNAME: " + (username != null && !username.isEmpty() ? "SET" : "NOT SET"));
            System.out.println("MAIL_PASSWORD: " + (password != null && !password.isEmpty() ? "SET" : "NOT SET"));
            return null; // Return null if not configured
        }
        
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);

        Properties props = mailSender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.starttls.required", "true");
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");
        props.put("mail.debug", "false");

        System.out.println("Email service configured successfully. Host: " + host + ", Port: " + port + ", Username: " + username);
        return mailSender;
    }
}
