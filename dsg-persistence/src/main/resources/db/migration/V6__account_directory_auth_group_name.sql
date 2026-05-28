ALTER TABLE account_directory_auth
    ADD COLUMN directory_group_name VARCHAR(256) NULL AFTER directory_group_id;
