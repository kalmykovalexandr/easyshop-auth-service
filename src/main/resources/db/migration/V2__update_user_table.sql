-- Update user table to match new User entity
ALTER TABLE auth.user 
  ADD COLUMN IF NOT EXISTS username VARCHAR(255),
  ADD COLUMN IF NOT EXISTS enabled BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS account_non_expired BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS account_non_locked BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS credentials_non_expired BOOLEAN NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ;

-- Rename password_hash to password to match entity
ALTER TABLE auth.user RENAME COLUMN password_hash TO password;

-- Update existing users to have username = email
UPDATE auth.user SET username = email WHERE username IS NULL;

-- Make username NOT NULL after updating existing records
ALTER TABLE auth.user ALTER COLUMN username SET NOT NULL;

-- Add unique constraint on username
ALTER TABLE auth.user ADD CONSTRAINT uk_user_username UNIQUE (username);

-- Create OAuth2 clients table
CREATE TABLE IF NOT EXISTS auth.oauth2_clients (
  id BIGSERIAL PRIMARY KEY,
  client_id VARCHAR(255) NOT NULL UNIQUE,
  client_secret VARCHAR(255),
  client_authentication_methods TEXT NOT NULL,
  authorization_grant_types TEXT NOT NULL,
  redirect_uris TEXT,
  scopes TEXT NOT NULL,
  client_settings TEXT,
  token_settings TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
  updated_at TIMESTAMPTZ
);

CREATE INDEX IF NOT EXISTS idx_oauth2_clients_client_id ON auth.oauth2_clients(client_id);
