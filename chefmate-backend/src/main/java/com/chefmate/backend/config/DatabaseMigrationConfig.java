package com.chefmate.backend.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class DatabaseMigrationConfig {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseMigrationConfig.class);

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void migrateDatabase() {
        try {
            // Check if enabled column exists
            String checkColumnSql = "SELECT COUNT(*) FROM information_schema.columns " +
                    "WHERE table_name = 'users' AND column_name = 'enabled'";
            
            Integer columnCount = jdbcTemplate.queryForObject(checkColumnSql, Integer.class);
            
            if (columnCount == null || columnCount == 0) {
                logger.info("Adding 'enabled' column to users table...");
                
                // Add the column
                jdbcTemplate.execute("ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true");
                
                // Update all existing users to be enabled
                jdbcTemplate.execute("UPDATE users SET enabled = true WHERE enabled IS NULL");
                
                logger.info("Successfully added 'enabled' column to users table");
            } else {
                logger.debug("Column 'enabled' already exists in users table");
            }
        } catch (Exception e) {
            logger.error("Error during database migration: {}", e.getMessage(), e);
            // Don't throw exception - allow application to start even if migration fails
            // Admin can manually add the column later
        }
    }
}
