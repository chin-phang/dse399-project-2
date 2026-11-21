--liquibase formatted sql

--changeset routeplanner:007-seed-admin
--comment Seed the initial ADMIN so a fresh environment has something to log in with.
--          BCrypt hash of "admin123" - change it immediately outside local development.
INSERT INTO app_user (username, password_hash, role)
VALUES ('admin', '$2a$10$dORKDdcggOKr6eHHlU.KwupWy3IDk7J.eb8betHAqbUMNee4yuuZe', 'ADMIN')
ON CONFLICT (username) DO NOTHING;
