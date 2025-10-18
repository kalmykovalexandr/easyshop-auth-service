-- Cleanup legacy email verification tables that are no longer used.
-- OTP codes now live entirely in Redis, so these tables must be removed.

DROP TABLE IF EXISTS auth.email_verification_code CASCADE;
DROP TABLE IF EXISTS auth.email_verification_token CASCADE;

-- Drop any leftover indexes explicitly (DROP TABLE with CASCADE should remove them,
-- but this keeps the migration idempotent when run on partially upgraded databases).
DROP INDEX IF EXISTS idx_email_verification_code_user;
DROP INDEX IF EXISTS idx_email_verification_code_expires;
DROP INDEX IF EXISTS idx_email_verification_code_attempts;
DROP INDEX IF EXISTS idx_email_verification_token_user;
DROP INDEX IF EXISTS idx_email_verification_token_expires;
