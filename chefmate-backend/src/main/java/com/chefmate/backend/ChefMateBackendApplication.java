package com.chefmate.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableJpaRepositories
public class ChefMateBackendApplication {
	
	private static final Logger logger = LoggerFactory.getLogger(ChefMateBackendApplication.class);
	
	public static void main(String[] args) {
		SpringApplication.run(ChefMateBackendApplication.class, args);
		logger.info("ChefMate Backend started successfully!");
		logger.info("Swagger UI: http://localhost:8090/swagger-ui/index.html");
		logger.info("API Docs: http://localhost:8090/api-docs");
	}
}