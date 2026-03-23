CREATE TABLE IF NOT EXISTS users (
    id            CHAR(36) PRIMARY KEY DEFAULT (UUID()),
    email         VARCHAR(255) UNIQUE NOT NULL,
    password_hash TEXT NOT NULL,
    full_name     VARCHAR(100),
    created_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS favorites (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     CHAR(36) NOT NULL,
    symbol      VARCHAR(20) NOT NULL,
    asset_type  ENUM('CRYPTO','STOCK') NOT NULL DEFAULT 'CRYPTO',
    added_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_user_symbol (user_id, symbol),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);