# Promptform AI

Promptform turns a plain-English workflow brief into a validated, usable form schema.

## Stack

- Next.js and React frontend
- Spring Boot API with SSE generation
- PostgreSQL with Flyway migrations
- Gemini provider with a local demo provider
- Optional Redis for shared rate limiting

## Run locally

1. Start Postgres: `docker compose up -d postgres`.
2. Set `JWT_SECRET` to a random value of at least 32 characters.
3. Keep `PROMPTFORM_LLM_PROVIDER=demo` for a zero-cost smoke test, or configure Gemini.
4. Run `npm install` and `npm run dev`.

Redis is disabled by default. If it is enabled and unavailable, generation falls back to the bounded in-memory limiter.

## Verification

```text
npm.cmd --prefix frontend run format:check
npm.cmd --prefix frontend run lint
npm.cmd --prefix frontend run build
mvn -f backend/pom.xml test
```

## Architecture

```text
Next.js workbench
        │ SSE / JSON
        ▼
Spring Boot API ── GenerationService ── Demo or Gemini provider
        │
        ├── SchemaValidator
        ├── optional Redis + in-memory fallback
        └── PostgreSQL / Flyway
```

See [DEPLOYMENT.md](DEPLOYMENT.md) for a free-tier deployment path using Vercel, Render, and Neon.
