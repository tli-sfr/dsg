-- RingCentral 3-legged OAuth refresh token (AES-256 encrypted at rest)
ALTER TABLE account_directory_auth
    ADD COLUMN rc_refresh_token VARCHAR(1024) NULL AFTER oauth_config_id;
