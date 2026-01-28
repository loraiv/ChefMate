package com.chefmate.backend.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI customOpenAPI() {
        final String securitySchemeName = "bearerAuth";

        // Server configuration
        Server localServer = new Server();
        localServer.setUrl("http://localhost:8090");
        localServer.setDescription("Local Development Server");

        // API Info
        Info info = new Info()
                .title("ChefMate API")
                .version("1.0.0")
                .description("""
                    ## ChefMate Recipe Management System API
                    
                    **Features:**
                    - User authentication (Register/Login with JWT)
                    - Recipe CRUD operations
                    - Shopping list management
                    - Image upload for recipes
                    
                    **Authentication:**
                    - Register to get an account
                    - Login to get JWT token
                    - Use token in Authorization header: `Bearer {token}`
                    """)
                .contact(new Contact()
                        .name("ChefMate Support")
                        .email("support@chefmate.com")
                        .url("https://chefmate.com"))
                .license(new License()
                        .name("MIT License")
                        .url("https://opensource.org/licenses/MIT"));

        // Security scheme for JWT
        SecurityScheme securityScheme = new SecurityScheme()
                .name(securitySchemeName)
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter your JWT token");

        return new OpenAPI()
                .info(info)
                .servers(List.of(localServer))
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, securityScheme));
    }
}