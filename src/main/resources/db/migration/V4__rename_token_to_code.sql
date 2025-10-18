-- Rename email verification table from token-based to code-based
ALTER TABLE IF EXISTS auth.email_verification_token
    RENAME TO email_verification_code;

-- Drop legacy column 'token' if it still exists
ALTER TABLE IF EXISTS auth.email_verification_code
    DROP COLUMN IF EXISTS token;

-- Ensure 'code' column exists (added previously in V3, keep idempotent)
ALTER TABLE IF EXISTS auth.email_verification_code
    ADD COLUMN IF NOT EXISTS code VARCHAR(8) NOT NULL;

-- Add attempts counter for OTP validation
ALTER TABLE IF EXISTS auth.email_verification_code
    ADD COLUMN IF NOT EXISTS attempts INT NOT NULL DEFAULT 0;

-- Drop unused 'used_at' column (we do not keep history)
ALTER TABLE IF EXISTS auth.email_verification_code
    DROP COLUMN IF EXISTS used_at;

-- Rename indexes to match new table name (if they exist)
ALTER INDEX IF EXISTS idx_email_verification_token_user
    RENAME TO idx_email_verification_code_user;

ALTER INDEX IF EXISTS idx_email_verification_token_expires
    RENAME TO idx_email_verification_code_expires;

-- (Re)create indexes defensively
CREATE INDEX IF NOT EXISTS idx_email_verification_code_user
    ON auth.email_verification_code(user_id);

CREATE INDEX IF NOT EXISTS idx_email_verification_code_expires
    ON auth.email_verification_code(expires_at);

-- Enforce single active code per user
CREATE UNIQUE INDEX IF NOT EXISTS uq_email_verification_code_user
    ON auth.email_verification_code(user_id);
