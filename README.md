# AI-Driven Logistics Route Planning Assistant

An enterprise logistics application where Route Planners manage delivery jobs and vehicles, and
use a conversational AI assistant — with tool calling and persistent conversation memory — to
generate route planning recommendations, view route summaries and export CSV reports.

| Layer | Technology |
|---|---|
| Frontend | React 19 + TypeScript + Tailwind 4 (Vite), Vercel AI SDK |
| Backend | Java 21 on Spring Boot 4.2, Maven |
| AI | Spring AI 2.1 → OpenAI `gpt-5.6-terra` |
| Database | PostgreSQL + PostGIS, migrated with Liquibase |

## Prerequisites

- JDK 21
- Node.js 20+
- Docker (for PostGIS, and for the container-backed integration tests)
- An OpenAI API key

## Quick start

**1. Start the database.** PostGIS, not plain Postgres — the schema enables the extension and
declares a generated `geography(Point,4326)` column.

```bash
docker compose -f docker/docker-compose.yml up -d
```

**2. Run the backend.** Liquibase applies the schema on first boot and seeds an `admin` user.

```bash
cd logistics-assistant && OPENAI_API_KEY=sk-your-key ./mvnw spring-boot:run
```

**3. Run the frontend.**

```bash
cd app-ui && npm install && npm run dev
```

Sign in with `admin` / `admin123` — change this immediately outside local development.

## Environment variables

| Variable | Default | Purpose |
|---|---|---|
| `OPENAI_API_KEY` | — | **Required.** OpenAI credentials. Never commit it. |
| `OPENAI_CHAT_MODEL` | `gpt-5.6-terra` | Set to `gpt-5.6-luna` if cost or rate limits bite — same tool-calling support, roughly 10× cheaper, at a real quality cost. |
| `SPRING_DATASOURCE_URL` | `jdbc:postgresql://localhost:5432/ai_logistics` | Database URL. |
| `SPRING_DATASOURCE_USERNAME` | `postgres` | Database user. |
| `SPRING_DATASOURCE_PASSWORD` | `postgres` | Database password. |
| `JWT_SECRET` | dev-only value | **Override in every real environment.** Base64, at least 256 bits. |
| `JWT_ACCESS_TTL` / `JWT_REFRESH_TTL` | `PT1H` / `P7D` | Token lifetimes (ISO-8601 durations). |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | Comma-separated allowed origins. |
| `AI_CONNECT_TIMEOUT` / `AI_READ_TIMEOUT` | `PT5S` / `PT30S` | Upstream HTTP guards, deliberately well above the 10s response NFR (see below). |
| `VITE_API_BASE_URL` (frontend) | `http://localhost:8080/api` | Backend base URL. |

## Commands

Run from `logistics-assistant/` (on Windows, `mvnw.cmd` is the same wrapper):

| Purpose | Command |
|---|---|
| Compile + run all tests | `./mvnw test` |
| Clean build | `./mvnw clean verify` |
| Run the app | `./mvnw spring-boot:run` |
| Single test class | `./mvnw test -Dtest=com.wou.logistics_assistant.SomeTest` |

Run from `app-ui/`: `npm run dev`, `npm run build`, `npm run lint`.

## API

All endpoints except `/api/auth/**` and `/actuator/health` require a bearer token.
The Bruno collection in `api/` exercises every one of them in order. Open that folder in Bruno, select the Local environment, and run the folders from top to bottom.

| Method | Path | Use case |
|---|---|---|
| POST | `/api/auth/login` · `/register` · `/refresh` | Authentication |
| POST | `/api/chat` | UC01–UC03 — streaming assistant (SSE) |
| GET POST PUT DELETE | `/api/jobs`, `/api/jobs/{id}` | UC04 |
| GET POST PUT DELETE | `/api/vehicles`, `/api/vehicles/{id}` | UC05 |
| POST | `/api/routes/plan` | UC06 — route recommendation |
| GET | `/api/routes/{id}/summary` | UC07 |
| GET | `/api/routes/{id}/export/csv` | UC08 |

## Notes on a few deliberate decisions

**Chat streaming.** `/api/chat` streams Server-Sent Events in the **AI SDK UI Message Stream
Protocol v1** (AI SDK 5+, verified against the pinned `ai@7`). This is *not* the older v4 "Data
Stream Protocol" — a confusingly similar name for a different wire format. Tool calls are merged
into the same stream as custom `data-toolCall` parts, which is what the AI Chat module's tool
panel renders.

**Conversation identity.** The chat-memory key is derived server-side as
`"<username>:<sessionId>"`. Memory lives in a shared Postgres table, so keying it on the
client-supplied session id alone would let any authenticated user read another user's
conversation.

**Tool context.** `@Tool` methods receive per-request state (the tool-event sink and the caller's
identity) through Spring AI's `ToolContext`. They must never read `SecurityContextHolder`: tools
execute on Reactor threads where the ThreadLocal-bound security context is empty, and would
silently see no user rather than failing.

**The ≤10s AI response NFR** is measured as *time to first token*, enforced client-side. The
`AI_*_TIMEOUT` values are upstream guards only and are set well above it, so a long but healthy
answer is not killed mid-stream.

**Geospatial data.** `latitude`/`longitude` on a job are the writable source of truth; `location`
is a DB-generated `geography(Point,4326)` column, GIST-indexed and mapped read-only. Distance
uses `ST_Distance` on geography (metres) — `ST_DistanceSphere` is geometry-only and would fail.
Coordinates are optional: a job created by the AI assistant arrives with only an address, and a
planner attaches the pin later in the Jobs module.

## Testing

`./mvnw test` runs the full suite. Tests that need a real PostGIS container are skipped —
visibly, as skipped — when no Docker daemon is reachable, and run in full when there is one.

| Test ID | Covers | Where |
|---|---|---|
| TC-FR01-001 / TC-FR02-001 | Assistant streaming and tool calling | `ChatServiceStreamTest` |
| TC-FR03-001 | Conversation memory scoping | `ConversationMemoryServiceTest` |
| TC-FR04-001 | Job management | `JobControllerTest` |
| TC-FR06-001 | Route planning heuristic | `PlanningServiceOrderingTest` |
| TC-FR07-001 | Route summary statistics | `RouteSummaryStatisticsTest` |
| TC-FR08-001 | CSV export | `ExportServiceImplTest` |
| TC-NFR-SEC-001 | AuthN/AuthZ | `SecurityAccessTest`, `JwtServiceTest` |

## Out of scope

Timefold Solver, GraphHopper, HERE Traffic, RAG, mobile apps, ERP/payment integrations, IoT
vehicle tracking. Route ordering is a nearest-neighbour heuristic, not a solver.
