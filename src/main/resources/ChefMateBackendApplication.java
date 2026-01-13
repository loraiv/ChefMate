package com.chefmate.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class ChefMateBackendApplication {
	public static void main(String[] args) {
		SpringApplication.run(ChefMateBackendApplication.class, args);
		System.out.println("ChefMate Backend started successfully!");
		System.out.println("Swagger UI: http://localhost:8090/swagger-ui/index.html");
		System.out.println("API Docs: http://localhost:8090/api-docs");
	}
}