# Screener

A real-time crypto market screener built with Angular 17+. It streams live candlestick data directly from Binance WebSocket APIs, lets authenticated users manage a personal watchlist of favorites, and renders interactive OHLCV charts in the browser — no server-side data proxy needed for market data.
 
---

## Features

- **Live candlestick charts** — connects to Binance WebSocket stream and merges incoming ticks with historical kline data in real time
- **Symbol & timeframe switching** — select any USDT-quoted pair from the full Binance exchange, across `1m / 5m / 15m / 1h / 4h / 1D` intervals
- **Favorites watchlist** — authenticated users can star/unstar symbols; state persists in MySQL via a Spring Boot REST API
- **JWT authentication** — register, login, and logout; the Angular auth interceptor automatically attaches the Bearer token to every API call
- **Route protection** — the auth guard redirects unauthenticated users away from protected routes
- **Exchange info cache** — the symbol list is cached in `localStorage` for 24 hours to avoid redundant API requests on every load

---

## Tech stack

| Layer | Technology |
|---|---|
| Frontend | Angular 17+ (standalone components, signals) |
| Backend | Spring Boot |
| Database | MySQL 8, Docker Compose |
| Market data | Binance WebSocket API  |
 
---

## Getting started

### Prerequisites

- Node.js 18+
- Angular CLI 17+
- Docker & Docker Compose

### 1 — Start the database
run docker then
```bash
docker compose up -d
```

This spins up MySQL 8 on port `3307` and runs `init.sql` tables.

| Setting | Value |
|---|---|
| Host | `localhost:3307` |
| Database | `screener_db` |
| User | `screener_user` |
| Password | `screener_pass` |

Inspect the database
Connect via the MySQL CLI inside the running container:
```
docker exec -it screener_db mysql -u screener_user -pscreener_pass screener_db
```

Useful queries once inside:
-- List all tables
SHOW TABLES;

-- Check holdings schema
DESCRIBE holdings;

-- Check transactions schema
DESCRIBE transactions;

-- View all holdings
SELECT * FROM holdings;

-- View all transactions
SELECT * FROM transactions;

-- Exit
EXIT;
```

### 2 — Start the backend

Start your Spring Boot application separately. It should expose:

```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/logout
GET  /api/favorites
POST /api/favorites
DELETE /api/favorites/:symbol
```

The Angular dev server proxies `/api/*` to the backend — configure `proxy.conf.json` if your backend runs on a non-default port.

### 3 — Start the frontend

```bash
npm install
ng serve
```

Navigate to `http://localhost:4200`.
 
---

## How market data works

The app connects to Binance entirely over WebSocket — no REST polling.

1. **On symbol/timeframe selection**, `MarketDataStore` calls `BinanceApiClient.connect()` to open a persistent stream subscription (`wss://stream.binance.com:9443/ws/{symbol}@kline_{interval}`).
2. Simultaneously, it requests the last 200 candles via the Binance WS API (`wss://ws-api.binance.com/ws-api/v3`, method `klines`).
3. Once historical data arrives, it is set as the initial candle array.
4. Each incoming live tick is merged: if its timestamp matches the last candle it replaces it (in-progress bar update); if it is newer it is appended (new bar).
5. The exchange info (full symbol list) is fetched once via WS API and cached in `localStorage` for 24 hours.

---

## Authentication flow

1. User submits the login form → `AuthService.login()` POSTs credentials to `/api/auth/login`.
2. The backend verifies the bcrypt password hash from MySQL and returns `{ token, email, fullName }`.
3. The token and user object are stored in `localStorage` under `lse_token` / `lse_user`.
4. Angular signals (`_currentUser`, `isLoggedIn`, `userInitials`) update reactively across the app.
5. All subsequent HTTP requests pass through `authInterceptor`, which clones the request and sets the `Authorization: Bearer <token>` header.
6. On logout, the token is removed from storage, the signal is cleared, and the router redirects to `/`.

---

## Database schema

```sql
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

CREATE TABLE transactions (
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
```
 
---

## Environment notes

- Market data is fetched directly from Binance in the browser — no API key required for public kline and exchange info endpoints.
- The Docker volume `mysql_data` persists database state across container restarts.
- The `init.sql` script only runs once on first container creation (`docker-entrypoint-initdb.d`). To reset, remove the volume: `docker compose down -v`.

---