CREATE TABLE IF NOT EXISTS auth.email_verification_token (
    id BIGSERIAL PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    user_id BIGINT NOT NULL REFERENCES auth."user"(id) ON DELETE CASCADE,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    code VARCHAR(8) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_email_verification_token_user ON auth.email_verification_token(user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_token_expires ON auth.email_verification_token(expires_at);
