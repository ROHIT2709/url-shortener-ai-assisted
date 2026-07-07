# URL Shortener (AI-Assisted Assessment)

A Spring Boot 3.5 / Java 17 URL shortener: shortens URLs to base62 codes, redirects with click tracking, enforces expiry, and exposes per-link statistics.

**Design highlights**

- **Base62 over a generated ID** — the short code encodes the database identity, not the URL itself (encoding a URL cannot shorten it).
- **302 redirect, not 301** — browsers don't cache the hop, so every click reaches the server and is counted.
- **Two-layer URL validation** — layer 1 syntactic (well-formed, http/https, host present, ≤ 2048 chars); layer 2 safety (blocks localhost, loopback, private/link-local/multicast IPs — SSRF guard).
- **Reserved paths** — `docs`, `h2`, `api`, `actuator` can never be captured as short codes.
- **Zero-config local dev** — every setting is an env var with an H2 fallback (`${VAR:default}` in `application.properties`); Docker injects Postgres values through the same variables.

---

## Prerequisites

| Mode | Needs |
|------|-------|
| Local (H2 in-memory) | JDK 17 (Maven comes via the `mvnw` wrapper) |
| Docker (PostgreSQL) | Docker Desktop / Engine with Compose v2 |

---

## Running

### 1. Local — H2, zero configuration

```bash
./mvnw spring-boot:run          # Windows: mvnw.cmd spring-boot:run
```

App starts on `http://localhost:8080`. Data lives in memory and resets on restart.

### 2. IntelliJ

Open the project, let Maven sync, run `UrlShortnerApplication`. No env vars needed.

### 3. Docker Compose — PostgreSQL

```bash
cp .env.example .env            # then edit POSTGRES_PASSWORD (required)
docker compose up --build
```

Brings up `postgres:16-alpine` (named volume, healthcheck) and the app (multi-stage image, non-root user). The app waits for the database to be healthy before starting.

---

## Endpoints

Interactive documentation (Swagger UI): **`http://localhost:8080/docs`**
Raw OpenAPI spec (importable into Postman): `http://localhost:8080/v3/api-docs`

### Shorten a URL

`POST /api/urls`

```json
{ "url": "https://example.com/very/long/path", "expiryDays": 7 }
```

`expiryDays` is optional (1–365, default 30).

| Status | Meaning |
|--------|---------|
| `201 Created` | Short URL created — body below |
| `400 Bad Request` | Blank/malformed/oversized URL, unsafe target (SSRF guard), or `expiryDays` out of range |

```json
{
  "shortCode": "1L9zO9O",
  "shortUrl": "http://localhost:8080/1L9zO9O",
  "originalUrl": "https://example.com/very/long/path",
  "createdAt": "2026-07-07T08:30:00Z",
  "expiresAt": "2026-07-14T08:30:00Z"
}
```

### Redirect

`GET /{shortCode}` — e.g. `http://localhost:8080/1L9zO9O`

| Status | Meaning |
|--------|---------|
| `302 Found` | Redirects to the original URL (`Location` header); click counted |
| `404 Not Found` | Unknown short code |
| `410 Gone` | Short URL has expired |

### Statistics

`GET /api/urls/{shortCode}/stats`

| Status | Meaning |
|--------|---------|
| `200 OK` | Body below |
| `404 Not Found` | Unknown short code |

```json
{
  "shortCode": "1L9zO9O",
  "originalUrl": "https://example.com/very/long/path",
  "createdAt": "2026-07-07T08:30:00Z",
  "expiresAt": "2026-07-14T08:30:00Z",
  "clickCount": 42,
  "expired": false
}
```

### Operational

| Path | Purpose |
|------|---------|
| `/actuator/health` | Health check |
| `/h2` | H2 console (local dev only; JDBC URL `jdbc:h2:mem:urlshortener`, user `sa`, blank password; disabled in Docker) |

All errors share one JSON shape:

```json
{ "timestamp": "...", "status": 404, "error": "Not Found", "message": "...", "path": "/abc123" }
```

---

## Configuration

Every value is an environment variable with a local-dev default (see `application.properties`):

| Variable | Default | Purpose |
|----------|---------|---------|
| `SERVER_PORT` | `8080` | HTTP port |
| `DB_URL` | H2 in-memory | JDBC URL (Compose sets Postgres) |
| `DB_DRIVER` | `org.h2.Driver` | JDBC driver class |
| `DB_USERNAME` / `DB_PASSWORD` | `sa` / empty | Database credentials |
| `DDL_AUTO` | `update` | Hibernate schema mode |
| `H2_CONSOLE_ENABLED` | `true` | H2 web console toggle |
| `APP_BASE_URL` | `http://localhost:${server.port}` | Base of generated short URLs |
| `APP_DEFAULT_EXPIRY_DAYS` | `30` | Expiry when `expiryDays` omitted |

Compose-level variables (database name/credentials, host ports) live in `.env` — see `.env.example`.

---

## Tests & CI

```bash
./mvnw test
```

55 tests: base62 codec, URL validator (both layers), service unit tests (Mockito), and end-to-end MockMvc integration tests (create → redirect → stats, error semantics, Swagger endpoints).

CI (`.github/workflows/ci.yml`) runs the full suite on every push/PR to `main` (JDK 17, Maven cache); surefire reports are uploaded as an artifact on failure.

---

## Project structure

```
src/main/java/com/rohit/url_shortner/
├── config/          OpenAPI (Swagger) configuration
├── controller/      REST API + redirect endpoint
├── dto/             Request/response records (bean-validated)
├── entity/          UrlMapping JPA entity
├── exception/       Typed exceptions + global handler (404/410/400 semantics)
├── repository/      Spring Data JPA
├── service/         Shorten / resolve / stats logic
├── util/            Base62 codec
└── validation/      Two-layer URL validator
```

## AI-assistance audit trail

Every prompt, decision, and correction during development is logged in
[`doc/prompt-journal.md`](doc/prompt-journal.md) (Problem / Resolution / Remark, timestamped).
