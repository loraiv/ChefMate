-- Add enabled column to users table if it doesn't exist
-- This migration adds the enabled field for user blocking functionality

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 
        FROM information_schema.columns 
        WHERE table_name = 'users' 
        AND column_name = 'enabled'
    ) THEN
        ALTER TABLE users ADD COLUMN enabled BOOLEAN NOT NULL DEFAULT true;
        -- Update all existing users to be enabled by default
        UPDATE users SET enabled = true WHERE enabled IS NULL;
    END IF;
END $$;
