CREATE TABLE users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    username VARCHAR(60) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    `role` VARCHAR(30) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id),
    CONSTRAINT uq_users_username UNIQUE (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE exhibits (
    id BIGINT NOT NULL AUTO_INCREMENT,
    image_url VARCHAR(1000) NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_exhibits PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE exhibit_translations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exhibit_id BIGINT NOT NULL,
    language_code VARCHAR(10) NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT pk_exhibit_translations PRIMARY KEY (id),
    CONSTRAINT uq_translation_exhibit_language
        UNIQUE (exhibit_id, language_code),
    CONSTRAINT fk_translation_exhibit
        FOREIGN KEY (exhibit_id) REFERENCES exhibits (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE qr_codes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    exhibit_id BIGINT NOT NULL,
    code VARCHAR(80) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_qr_codes PRIMARY KEY (id),
    CONSTRAINT uq_qr_codes_code UNIQUE (code),
    CONSTRAINT fk_qr_code_exhibit
        FOREIGN KEY (exhibit_id) REFERENCES exhibits (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE audio_assets (
    id BIGINT NOT NULL AUTO_INCREMENT,
    translation_id BIGINT NOT NULL,
    provider VARCHAR(40) NOT NULL,
    voice_code VARCHAR(20) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    format VARCHAR(10) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_audio_assets PRIMARY KEY (id),
    CONSTRAINT uq_audio_asset_cache
        UNIQUE (translation_id, provider, voice_code, content_hash, format),
    CONSTRAINT fk_audio_asset_translation
        FOREIGN KEY (translation_id) REFERENCES exhibit_translations (id)
        ON UPDATE CASCADE
        ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;