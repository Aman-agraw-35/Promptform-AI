# Free deployment

This project is designed to run without a paid Redis instance. Redis is an optional acceleration layer for shared rate limits; the API starts and continues serving traffic with the local in-memory limiter when Redis is disabled or unavailable.

## Local development

1. Start Postgres with `docker compose up -d postgres`.
2. Leave `REDIS_ENABLED=false` unless a local Redis server is available.
3. Start the application with `npm run dev`.
4. Use `PROMPTFORM_LLM_PROVIDER=demo` for a zero-cost local smoke test, or set `PROMPTFORM_LLM_PROVIDER=gemini` and `GEMINI_API_KEY` for real generation.

## Free hosted setup

- Deploy `frontend` as a Next.js project on Vercel Hobby.
- Deploy the backend from the repository root on Render Free using `render.yaml`.
- Create a free Postgres database on Neon and provide its JDBC connection string as `DATABASE_URL`, plus the database username and password.
- Keep `REDIS_ENABLED=false`. If shared distributed rate limiting becomes necessary later, set `REDIS_ENABLED=true` and provide `REDIS_URL` for a compatible Redis service.
- Set `NEXT_PUBLIC_API_URL` on Vercel to the public Render API URL.

## Required backend variables

`DATABASE_URL` must begin with `jdbc:postgresql://`. Set `DATABASE_USERNAME`, `DATABASE_PASSWORD`, and a long random `JWT_SECRET`. Use `PROMPTFORM_LLM_PROVIDER=demo` for a free demo or `gemini` with `GEMINI_API_KEY` for live generation.

Set the backend CORS origin with `PROMPTFORM_FRONTEND_ORIGIN` when the frontend is hosted on a different domain.

## Free-tier expectations

Free web services may sleep when idle, cold starts are expected, and hosted free databases have storage/compute limits. This setup is suitable for a portfolio demo, testing, and small usage; it is not a production availability guarantee. Never commit real secrets or API keys.
