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

CREATE TABLE IF NOT EXISTS holdings (
    id          BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id     CHAR(36) NOT NULL,
    symbol      VARCHAR(20) NOT NULL,
    total_qty   DECIMAL(18,8) NOT NULL DEFAULT 0,
    avg_price   DECIMAL(18,8) NOT NULL DEFAULT 0,
    asset_type  ENUM('CRYPTO','STOCK') NOT NULL DEFAULT 'CRYPTO',
    notes       VARCHAR(500),
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_holding_user_symbol (user_id, symbol),
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS transactions (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    holding_id  BIGINT NOT NULL,
    type        ENUM('BUY','SELL') NOT NULL,
    quantity    DECIMAL(18,8) NOT NULL,
    price       DECIMAL(18,8) NOT NULL,
    note        VARCHAR(500),
    date        DATE NOT NULL,
    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (holding_id) REFERENCES holdings(id) ON DELETE CASCADE
);