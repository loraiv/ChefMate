package com.chefmate.backend.dto;

public class LoginRequest {
    private String usernameOrEmail;
    private String password;

    // Getters
    public String getUsernameOrEmail() { return usernameOrEmail; }
    public String getPassword() { return password; }

    // Setters
    public void setUsernameOrEmail(String usernameOrEmail) { this.usernameOrEmail = usernameOrEmail; }
    public void setPassword(String password) { this.password = password; }
}