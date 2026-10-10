ALTER TABLE users
    ADD COLUMN token_version BIGINT NOT NULL DEFAULT 0;

-- Earlier DEV bootstrap code stored the Spring authority prefix in the role column.
UPDATE users SET `role` = 'ADMIN' WHERE `role` = 'ROLE_ADMIN';
