# Promptform-AI: Implementation Plan

> Describe it. Promptform builds it.

Promptform-AI turns a plain-English prompt ("insurance claim form with conditional fields for vehicle type") into a validated JSON UI schema and renders it live. Users refine it with follow-up prompts, browse version history, and export the result.

**Repo:** https://github.com/Aman-agraw-35/Promptform-AI

---

## How to use this plan (read first)

- Work **one phase at a time**, in order. Do not start a phase until the previous phase's "Done when" is true.
- Follow **Engineering Principles** and **Git and Commit Workflow** below for every change.
- **Ask me before every commit and every push.** Ask me before major or hard-to-reverse decisions (schema shape, new dependencies, API contracts).
- For non-trivial tasks, state your chosen approach and the alternative you rejected in the chat before coding.
- Keep this file as the source of truth. If a decision changes the plan, tell me and propose the edit instead of silently drifting.

---

## 1. Goals

- Ship a deployed, polished full-stack AI product, not a tutorial demo.
- Show production-grade thinking: schema-validated LLM output, streaming, evals, observability.
- Cover both **React/TypeScript** and **Spring Boot** skill sets.
- Collect real usage numbers (users, validity rate, latency) for the resume.

## 2. Tech Stack

| Layer | Choice |
|---|---|
| Frontend | Next.js, TypeScript, Tailwind, shadcn/ui, react-hook-form, Zod, Framer Motion |
| Backend | Spring Boot (Java 21), Spring Security (JWT/OAuth), Spring Data JPA, Flyway |
| AI | Gemini API (Google GenAI), structured JSON output, streaming, behind an `LLMProvider` interface |
| Data | PostgreSQL (JSONB for schemas), Redis (rate limiting + caching) |
| Contract | JSON Schema as the single source of truth for the UI schema |
| CI | GitHub Actions (lint, build, evals) |
| Observability | Spring Actuator + Micrometer, simple cost/latency logging |
| Deploy | Vercel (frontend), Render/Railway/Fly.io (backend + Postgres) |

## 3. Architecture

```
Browser (Next.js)
   │  REST + SSE
   ▼
Spring Boot API ──► Gemini API
   │   ├─ Auth (JWT/OAuth)
   │   ├─ Generation service (prompt → JSON → validate → repair)
   │   ├─ Versioning service (history, diffs, share links)
   │   └─ Eval runner (async)
   ▼
PostgreSQL + Redis
```

**Core loop:** prompt → Gemini returns a JSON UI schema → backend validates it → on failure, repair with the exact issues → stream the validated result → client renders it from a component registry.

## 4. Repo Structure

```
Promptform-AI/
├── frontend/        # Next.js app
├── backend/         # Spring Boot app
├── schema/          # JSON Schema (source of truth) + type generation scripts
├── evals/           # prompt test set + scoring scripts
├── docs/            # architecture diagram, screenshots, demo GIF
├── docker-compose.yml
├── Plan.md
└── README.md
```

## 5. UI Schema and Rendering Design

The schema is a tree of typed nodes. The renderer maps each node's `type` to a React component through a registry.

```json
{
  "title": "Vehicle claim",
  "children": [
    { "type": "text", "name": "fullName", "label": "Full name", "required": true },
    { "type": "radio", "name": "vehicleType", "label": "Vehicle type",
      "options": [{ "value": "car", "label": "Car" }, { "value": "bike", "label": "Bike" }] },
    { "type": "number", "name": "engineCc", "label": "Engine capacity (cc)",
      "visibleWhen": { "field": "vehicleType", "equals": "bike" } }
  ]
}
```

**Node families**
- **Input:** text, email, number, textarea, date, slider. Share one label/hint/error wrapper.
- **Option-based:** radio, select, checkbox group. An MCQ is a `radio` (one answer) or `checkboxGroup` (many); no separate type.
- **Layout:** section, columns, tabs, heading, divider. Containers render their `children` through the same renderer, so nesting is free.
- **Dashboard:** stat card, chart, table. These read from a data source defined in the schema instead of form state.

**Rendering rules**
- A single registry (`type → component`) is the only place that knows which types exist. Adding a type means: extend the JSON Schema, write the component, add one registry entry.
- Unknown types render a safe `UnknownNode` fallback. Each node sits inside its own error boundary, so one bad field never breaks the whole form.
- Form state uses react-hook-form, keyed by field `name`.
- Validation rules (`required`, `min`, `max`, `pattern`) are turned into a Zod schema at runtime from the node tree, so the rules the model wrote are actually enforced.
- Conditions are **declarative JSON** (`visibleWhen`), evaluated against live form values. Never `eval` model output and never render it as raw HTML.

## 6. Generation Correctness Pipeline

Treat model output as untrusted input. Nothing reaches the renderer without passing every stage.

```
prompt → Gemini → parse → structural check → semantic check → normalize → render
              ▲                     │ any failure
              └── repair prompt ◄───┘  (max 2 retries, then a clean error)
```

1. **Constrain up front:** send a response schema limited to supported types, low temperature, a system prompt listing supported components with examples. Check the finish reason; a truncated response is retried, not parsed.
2. **Structural validation:** JSON Schema check. Each failure becomes a `ValidationIssue` with a path, code, and message.
3. **Semantic validation** (separate `SchemaValidator` service):
   - duplicate field `name` values
   - `visibleWhen` pointing to a missing field, or circular conditions
   - option-based fields with no options or duplicate option values
   - `min` greater than `max`, invalid regex, default value not among the options
   - required fields that can never become visible
   - limits on nesting depth and total field count
4. **Repair loop:** send the original prompt, the previous output, and the exact issues. Fix trivial problems deterministically in code (for example, deduplicating names) without another model call. After two failed retries, return a `ProblemDetail` error. Never show a broken form.
5. **Streaming:** show partial output as a draft; when the backend's final validated result arrives, replace the draft. If validation fails, discard the draft and show the error or repair state.
6. **Refinement prompts:** validate like any other result, and also check that the edit did not delete or rename fields it was not asked to touch.
7. **Measure:** log every issue by code; track first-try validity and repair success rate. These feed the evals and the resume metrics.

## 7. Data Model and API (high level)

**Tables:** `users`, `projects`, `versions` (project, parent version, schema JSONB, prompt, validation metadata, created at), `share_links`, `eval_runs`, `eval_results`.

**Endpoints**
- Auth: login (JWT), OAuth callback, current user
- `POST /api/projects`, `GET /api/projects`, `GET /api/projects/{id}`
- `POST /api/projects/{id}/generate` (SSE): first generation from a prompt
- `POST /api/projects/{id}/refine` (SSE): follow-up edit on the current version
- `GET /api/projects/{id}/versions`, `GET /api/versions/{id}`, `POST /api/versions/{id}/fork`
- `POST /api/projects/{id}/share`, `GET /api/share/{token}` (public, read-only)
- `POST /api/evals/run`, `GET /api/evals/runs`

All responses use DTOs. All errors use one consistent `ProblemDetail` shape.

## 8. Phases

Each phase ends with a working, demoable slice. Timeline is about 5-6 weeks.

### Phase 0: Setup (day 1-2)
- Initialize monorepo, `.gitignore`, README stub, LICENSE.
- Scaffold Next.js and Spring Boot apps; Docker Compose for Postgres + Redis.
- Set up CI (lint + build on every PR).
- Keep API keys in `.env` files that are never committed; commit only `.env.example`.
- **Done when:** both apps start locally with one command and CI is green.

### Phase 1: Thin vertical slice (week 1)
- Minimal JSON UI schema: text, number, select, radio, checkbox group, section.
- Spring endpoint: prompt → Gemini → JSON.
- Frontend renderer with the component registry and a prompt box.
- Build the SSE streaming path now, even if it is basic.
- **Done when:** typing a prompt shows a rendered form on screen.

### Phase 2: Reliability and conditional logic (week 2)
- Full generation correctness pipeline (section 6): structural and semantic validation, repair loop, deterministic fixes.
- Expand schema: validation rules, `visibleWhen`, columns, tables and charts for dashboards.
- Generate TypeScript types and Zod from the JSON Schema; CI check for drift.
- **Done when:** invalid model output is caught and repaired or rejected with a clear error, and conditional fields work.

### Phase 3: Product features (week 3)
- Auth (JWT + Google/GitHub login).
- Projects and versions in Postgres; undo, compare (diff), fork.
- Refinement prompts that edit the current schema.
- Share links and read-only embed view.
- **Done when:** a user can log in, generate, refine, roll back, and share a form.

### Phase 4: UI polish (week 4)
- Split-pane layout: prompt/chat left, live preview right.
- Desktop/mobile preview toggle, dark mode, JSON and code tabs.
- Export generated React/TypeScript code.
- Loading, error, and empty states; keyboard shortcuts; subtle animation. Clean, minimal design in the style of Linear or Vercel.
- **Done when:** the app looks and feels demo-ready on desktop and mobile.

### Phase 5: Evals and observability (week 5)
- 30-50 prompt test set with auto-scoring: schema valid, renders without errors, meets stated requirements.
- Async eval runner; results stored and shown on a simple dashboard.
- Run evals in GitHub Actions on every PR; track pass rate over time.
- Metrics: latency, token cost, retry rate, failure rate.
- **Done when:** the pass rate is visible in CI and on the dashboard.

### Phase 6: Hardening, deploy, launch (week 5-6)
- Redis rate limiting (per user), prompt caching, input sanitization.
- Deploy, add a custom domain if possible.
- README with architecture diagram, 60-second demo GIF, and metrics.
- Share with 20-50 real users; collect feedback and numbers.
- **Done when:** the public demo link works and the README is complete.

## 9. Success Metrics (for the resume)

- First-try valid schema rate and rate after repair (target 90%+ and 97%+).
- Median generation time (target under 4 s).
- Eval pass rate across the test set.
- Number of real users and generated forms.

Example bullet: *"Built an AI UI generator (Next.js, Spring Boot, Postgres, Redis) with schema-validated Gemini output, SSE streaming, versioned history, and a CI eval suite (X% pass rate over N prompts); deployed with N+ users."*

---

## 10. Engineering Principles (agent instructions)

Act like a senior software architect while building this project. The best solution to a problem is rarely more code; it is the right approach.

### Think before coding
- Understand the problem and its constraints first. Then consider two or three approaches and pick the simplest one that fully solves it.
- Before writing code for anything non-trivial, state the chosen approach and the main alternative you rejected, with the reason, in the chat (never in the code). For major or hard-to-reverse choices (schema changes, new dependencies, API shape), ask me before proceeding.
- Prefer reusing, deleting, or simplifying existing code over adding new code. Adding a library or abstraction needs a clear reason.
- Solve the problem at the right layer. If a bug keeps needing patches, fix the design instead.
- Follow KISS and YAGNI: build what the current phase needs, not what might be needed later.

### Code quality
- Write clean, readable, maintainable code that looks like an experienced human wrote it: consistent style, natural structure, no filler.
- **No comments.** Make intent clear through good names, small focused functions, and clear structure. Tooling directives that a tool requires are the only exception.
- Meaningful names for everything; no magic numbers or strings (use named constants or config).
- Small functions and classes with a single responsibility; shallow nesting; early returns over deep conditionals.
- No dead code, no commented-out code, no TODO litter, no placeholder text.
- Avoid over-engineering: no speculative interfaces, factories, or layers without a real need. Keep the one abstraction that matters (`LLMProvider`) and don't add others without a reason.
- Don't copy and paste logic. Extract it when the same logic appears a third time, not before.

### Error handling
- Validate input at the boundaries (controllers, API routes, form inputs). Trust internal code after that.
- Use specific, meaningful exception types; never swallow errors, and never use an empty catch.
- **Spring Boot:** one global `@RestControllerAdvice` that maps exceptions to a consistent error response (RFC 7807 `ProblemDetail`). Never leak stack traces or internals to clients.
- **Gemini calls:** set timeouts, retry transient failures with backoff, handle rate limits and malformed output explicitly, and fail with a clear error when retries run out.
- **Frontend:** error boundaries, typed API errors, and friendly messages with a way to retry. Loading, empty, and error states are part of every feature.
- Log failures once, at the right level, with useful context and no secrets.

### System design principles
- **Separation of concerns:** controller → service → repository on the backend; UI components separate from data fetching and state on the frontend.
- **SOLID** where it earns its keep, especially dependency inversion at the AI provider boundary.
- Keep the API stateless, configuration in environment variables (12-factor), and secrets out of the repo.
- Design endpoints to be idempotent where it makes sense; use pagination for lists and DTOs instead of exposing entities.
- Make database changes through Flyway migrations only; add indexes for the queries you actually run.
- Apply least privilege and secure defaults: authenticated routes by default, rate limits on expensive endpoints, sanitized input.
- Keep the JSON Schema as the single contract between frontend, backend, and the LLM.

### Before calling a task done
- Does it solve the actual problem in the simplest reasonable way?
- Would another developer understand it without a single comment?
- Are failure cases handled and surfaced properly?
- Does it respect the layering and contract rules above?
- Is the diff as small as it can be?

---

## 11. Git and Commit Workflow (agent instructions)

The goal is a clean, believable history that shows steady progress. Any coding agent working on this repo must follow these rules.

### First-time setup
```bash
echo "# Promptform-AI" >> README.md
git init
git add README.md
git commit -m "first commit"
git branch -M main
git remote add origin https://github.com/Aman-agraw-35/Promptform-AI.git
git push -u origin main
```

### Core rule: always ask first
After finishing a minor or major change, **stop and ask me before committing**. Show:
1. A short summary of what changed.
2. The files that will be staged.
3. The proposed commit message.

Only run `git commit` after I say yes. Ask separately before `git push`. Never commit or push on your own.

### What counts as a commit point
- **Minor:** a single component, endpoint, bug fix, config tweak, or doc update that works on its own.
- **Major:** a finished feature or phase milestone (for example, "streaming works end to end").
- Don't bundle unrelated changes. If a change touches two concerns, propose two commits.
- Don't commit broken builds. Make sure it compiles and runs first.

### Message style (humanized)
- Short, lowercase, imperative, about 50-70 characters, no trailing period.
- Say what changed in plain words, the way a developer actually writes.
- Add a one-or-two-line body only when the *why* isn't obvious.
- Avoid robotic or inflated wording ("implement comprehensive solution", "enhance functionality").
- Light prefixes like `fix:` or `docs:` are fine, but don't use them on every commit.
- No emojis, no AI co-author trailers unless I ask for them.

**Good examples**
```
add basic form renderer for text and select fields
wire gemini call into generation endpoint
fix sse stream closing early on long responses
retry with validation errors when schema check fails
add flyway migration for versions table
tidy prompt box styling, fix dark mode contrast
write first 10 eval prompts
update readme with setup steps
```

**Avoid**
```
Implemented comprehensive AI generation functionality with enhanced error handling
update files
changes
final fix
```

### Branching
- `main` always stays working.
- For each phase or larger feature, use a branch like `feat/streaming` or `fix/sse-timeout`, then merge into `main` (a PR on GitHub is nice, even solo).
- Small docs or config changes can go straight to `main` after I approve.

### Safety checks before every commit
- No API keys, `.env` files, or credentials in the diff (check for the Gemini key especially).
- No build output, `node_modules`, or IDE files.
- Run lint and make sure the part you changed builds.

### Pace
- Aim for small, frequent commits spread across the days I actually work, not one giant dump.
- Tag milestones when a phase is complete: `v0.1` (Phase 1), `v0.2` (Phase 2), and so on, with `v1.0` at launch.

---

## 12. Risks and Mitigations

| Risk | Mitigation |
|---|---|
| Gemini rate limits or quota during evals | Use a fast, cheap model for evals; cache results; throttle runner |
| Invalid or malformed LLM output | Correctness pipeline (section 6), repair loop, eval tracking |
| Streaming complexity in Spring | Build the SSE path in Phase 1, before adding features |
| Frontend/backend schema drift | Generate types from `schema/`, fail CI on mismatch |
| Malicious or oversized model output | Depth and field-count limits, safe regex handling, no raw HTML or `eval` |
| Scope creep | Finish each phase's demoable slice before starting the next |

## 13. Definition of Done

- Deployed and publicly accessible with a demo link.
- README with architecture diagram, demo GIF, metrics, and setup instructions.
- Eval suite running in CI with a visible pass rate.
- At least 20 real users and a few pieces of feedback captured.
