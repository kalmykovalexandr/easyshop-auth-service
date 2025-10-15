-- Migration: Refactor email verification to use hashed codes
-- This migration renames 'code' column to 'code_hash' and extends its length for BCrypt hashes

-- Step 1: Rename the 'code' column to 'code_hash'
ALTER TABLE auth.email_verification_code
    RENAME COLUMN code TO code_hash;

-- Step 2: Extend the column length to 60 characters to accommodate BCrypt hashes
-- BCrypt hashes are 60 characters long (e.g., $2a$10$...)
ALTER TABLE auth.email_verification_code
    ALTER COLUMN code_hash TYPE VARCHAR(60);

-- Step 3: Ensure attempts column exists (idempotent - already added in V4)
-- This is a safety check in case V4 was not run
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'auth'
          AND table_name = 'email_verification_code'
          AND column_name = 'attempts'
    ) THEN
        ALTER TABLE auth.email_verification_code
            ADD COLUMN attempts INT NOT NULL DEFAULT 0;
    END IF;
END $$;

-- Step 4: Add index on attempts for performance (optional but recommended)
CREATE INDEX IF NOT EXISTS idx_email_verification_code_attempts
    ON auth.email_verification_code(attempts);

-- Step 5: Clean up any expired codes before applying new constraints
DELETE FROM auth.email_verification_code
WHERE expires_at < NOW();

-- Note: Existing plain-text codes in the database will need to be re-generated
-- Users with pending verification codes will need to request a new code after this migration
-- This is acceptable for security upgrade
