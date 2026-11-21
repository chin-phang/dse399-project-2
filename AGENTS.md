# AGENTS.md

Guidance for agents working in this repository.

## Project Overview

**AI-Driven Logistics Route Planning Assistant** — an enterprise logistics application
that lets Route Planners manage delivery jobs and vehicles, and use a conversational
AI assistant (with tool calling and conversation memory) to generate route planning
recommendations, view route summaries, and export reports.

Core capabilities:
- AI Conversational Assistant (natural language interface)
- AI Tool Calling (GPT invokes backend business functions)
- Conversation Memory (context retained across a session)
- Delivery Job management
- Vehicle / fleet management
- AI-assisted route planning recommendations
- Route summary reporting
- CSV route report export

## Tech Stack

| Layer | Technology | Notes |
|---|---|---|
| Frontend | React + TypeScript + Tailwind CSS | Component-based UI |
| AI Frontend | AI SDK by Vercel (`ai` package, v5.x — pin the exact version) | Streaming chat via the AI SDK **UI Message Stream Protocol v1** (SSE). This is the UI Message Stream (`x-vercel-ai-ui-message-stream: v1`, `data: {"type":...}` frames terminated by `data: [DONE]`). The older AI SDK v4 "Data Stream Protocol" (`x-vercel-ai-data-stream`, `0:"…"` frames) is a different wire format; do not follow v4 docs. |
| Backend language | Java 21 | Maven project, `java.version` 21 |
| Backend framework | Spring Boot 4.2 | REST APIs, DI, `spring-boot-starter-webmvc` (Servlet stack — SSE is served from `Flux` return types without a WebFlux server) |
| AI orchestration | Spring AI 2.1 | GPT integration, Tool Calling (`ToolCallingAdvisor`, auto-registered), Conversation Memory (JDBC-backed `ChatMemoryRepository`) |
| LLM | OpenAI GPT — `gpt-5.6-terra` | Pinned via `spring.ai.openai.chat.options.model: ${OPENAI_CHAT_MODEL:gpt-5.6-terra}`; `gpt-5.6-luna` is the documented cost/quota fallback (set `OPENAI_CHAT_MODEL`) |
| Database | PostgreSQL | Transactional storage; migrations via Liquibase (`db/changelog/`) |
| Spatial extension | PostGIS | `hibernate-spatial` + `jts-core` on the Maven classpath. Delivery coordinates on `Job` back a DB-generated `geography(Point,4326)` column, GIST-indexed. Distance is `ST_Distance` on `geography` (metres, spheroidal). `ST_DistanceSphere` is geometry-only and fails with `function st_distancesphere(geography, geography) does not exist`. |
| Base package | `com.wou.logistics_assistant` | |

### Repository layout

| Directory | Contents |
|---|---|
| `logistics-assistant/` | Maven Java project. All backend code. Wrapper is `mvnw` / `mvnw.cmd`. |
| `app-ui/` | The React app. **Already scaffolded** — it is the *ecme* admin template (React 19, Tailwind 4, react-router 7, zustand, SWR, axios), not a bare `create vite` project. Extend it; do not scaffold a second frontend. |
| `docker/` | `docker-compose.yml` for local infrastructure (PostGIS). |

Working inside `app-ui/`, follow the template's own conventions rather than inventing
parallel ones: REST clients go under `src/services/`, feature screens under `src/views/`,
shared state in `src/store/`, config in `src/configs/`. The template ships a mock auth layer
(`axios-mock-adapter` + Firebase) that must be **replaced** by the real JWT backend, not left
running alongside it.

**Out of scope for this codebase** (do not implement unless explicitly asked):
Timefold Solver, GraphHopper, HERE Traffic API, RAG, mobile apps, ERP/payment
integrations, IoT vehicle tracking.

## Architecture

Layered architecture with four principal layers — keep new code inside the layer it
belongs to and respect the direction of dependencies:

```
Presentation (React/TS)
      ↓ REST
Application Layer (Spring Boot controllers + business services)
      ↓
AI Service Layer (Spring AI: ChatService, ToolCallingService, ConversationMemoryService, PlanningService)
      ↓
Data Layer (Repositories → PostgreSQL / PostGIS)
```

Key rule: **AI logic stays isolated from core business logic.** `PlanningService`
(AI-assisted optimization) never touches repositories directly — it always delegates
persistence and route lifecycle operations to `RouteService`. This keeps optimization
logic swappable (future Timefold/GraphHopper integration) without touching business
services.

## Package Structure

```
com.wou.logistics_assistant
│
├── config/
│   └── OpenAiConfig.java   # ChatClient bean, LogisticsTools registration, JDBC-backed ChatMemory bean
│
├── controller/
│   ├── AuthController.java
│   ├── ChatController.java      # POST /api/chat streams SSE (ChatService.chatStream); no separate sync route by default
│   ├── JobController.java
│   ├── RouteController.java     # also hosts /plan, /{id}/summary, /{id}/export/csv — no separate RouteSummary/Export controllers
│   └── VehicleController.java
│
├── dto/
│   ├── auth/        (LoginRequest, LoginResponse, RegisterRequest, RefreshTokenRequest)
│   ├── job/
│   ├── route/
│   ├── vehicle/
│   └── chat/
│
├── entity/
│   ├── User.java
│   ├── Role.java
│   ├── Vehicle.java
│   ├── Job.java
│   ├── Route.java
│   └── RouteSummary.java
│
├── repository/
│   ├── UserRepository.java
│   ├── VehicleRepository.java
│   ├── JobRepository.java
│   ├── RouteRepository.java
│   └── RouteSummaryRepository.java
│
├── security/
│   ├── config/
│   │   ├── SecurityConfiguration.java
│   │   └── CorsConfiguration.java
│   ├── jwt/
│   │   ├── JwtService.java
│   │   ├── JwtAuthenticationFilter.java
│   │   ├── JwtAuthenticationEntryPoint.java
│   │   └── JwtAccessDeniedHandler.java
│   ├── util/
│   │   └── SecurityUtils.java
│   └── CustomUserDetailsService.java
│
├── service/
│   ├── AuthService.java
│   ├── ChatService.java
│   ├── ConversationMemoryService.java
│   ├── ExportService.java
│   ├── JobService.java
│   ├── PlanningService.java
│   ├── RouteService.java
│   ├── RouteSummaryService.java
│   ├── ToolCallingService.java
│   ├── UserService.java
│   ├── VehicleService.java
│   └── impl/
│       ├── AuthServiceImpl.java
│       ├── ChatServiceImpl.java
│       ├── ConversationMemoryServiceImpl.java
│       ├── ExportServiceImpl.java
│       ├── JobServiceImpl.java
│       ├── PlanningServiceImpl.java
│       ├── RouteServiceImpl.java
│       ├── RouteSummaryServiceImpl.java
│       ├── ToolCallingServiceImpl.java
│       ├── UserServiceImpl.java
│       └── VehicleServiceImpl.java
│
├── tool/          (AI-invokable @Tool definitions, e.g. LogisticsTools.java)
├── exception/
├── mapper/        (entity ↔ DTO mapping only, Mappers.java)
└── util/          (e.g. AiSdkStreamEncoder — builds the SSE frames ChatController streams; not a mapper concern)
```

Use the `service/impl/` subpackage form (not `service/AuthServiceImpl.java` flat) —
it keeps contracts and implementations visually separated in the IDE tree.

API DTOs are Java records. JPA entities are classes with an explicit no-arg constructor
for Hibernate plus a constructor the application code calls.

## Service Layer Conventions

**Default rule:** define an interface only where it adds real value. Do not create
an interface + impl pair for every class by default — that produces boilerplate
with no second implementation ever materializing.

### Business services → always get an interface

Controllers must depend on the interface, never the impl, so implementations stay
swappable and mockable in tests.

```java
public interface VehicleService {
    VehicleDto registerVehicle(CreateVehicleRequest request);
    VehicleDto getVehicle(Long vehicleId);
    List<VehicleDto> getVehicles();
    VehicleDto updateVehicle(Long vehicleId, UpdateVehicleRequest request);
    void deleteVehicle(Long vehicleId);
}

@Service
@Transactional
public class VehicleServiceImpl implements VehicleService {
    private final VehicleRepository vehicleRepository;

    public VehicleServiceImpl(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }
}

@RestController
public class VehicleController {
    private final VehicleService vehicleService; // depend on interface only

    public VehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }
}
```

Constructor injection is an explicit constructor assigning `private final` fields.
There is no Lombok and no `@RequiredArgsConstructor` in this codebase.

### Infrastructure adapters → skip the interface unless there's a real reason to abstract

Single-implementation infrastructure code (JWT signing, password hashing, one-off
adapters) doesn't need an interface layer on top.

```java
@Service
public class JwtService {
    public String generateAccessToken(UserDetails user) { ... }
    public String extractUsername(String token) { ... }
    public boolean isTokenValid(String token, UserDetails user) { ... }
    public boolean isExpired(String token) { ... }
    public Date extractExpiration(String token) { ... }
}
```

`JwtService` must only create/validate tokens — it never authenticates users,
queries the database, or calls repositories.

### Spring/Spring Security already provides the interface

Don't wrap these in a second, redundant interface:

```java
@Service
public class CustomUserDetailsService implements UserDetailsService {
    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) { ... }
}
```

`CustomUserDetailsService` only loads a user and maps `User → UserDetails`. It never
generates a JWT.

For password hashing, inject Spring's own interface — don't invent a `PasswordService`:

```java
@Bean
public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
}
```

### Interface decision table

| Component | Interface? | Reason |
|---|---|---|
| VehicleService, JobService, PlanningService, ChatService, ToolCallingService, UserService, ExportService, RouteService, RouteSummaryService, ConversationMemoryService, AuthService | Yes | Business service (use case) |
| JwtService | No | Infrastructure utility, single implementation |
| CustomUserDetailsService | No | Already implements Spring's `UserDetailsService` |
| PasswordEncoder | No | Spring already provides the interface |
| AuthenticationManager | No | Spring already provides the interface |
| VehicleRepository / other repositories | No | Already an interface (`JpaRepository`) |

Guideline to extend this table for new components: **business services (application
use cases) get interfaces; infrastructure adapters (JWT, email, file storage, cache,
external API clients) start as concrete classes** unless multiple implementations
are genuinely anticipated.

## Security / JWT Flow

`AuthService` owns the login/register use case and orchestrates the other security
pieces — it is the only place a JWT is actually generated:

```
AuthController → AuthServiceImpl
    → AuthenticationManager.authenticate(...)
    → CustomUserDetailsService.loadUserByUsername(...)
    → JwtService.generateAccessToken(user)
    → return LoginResponse(token)
```

Request-time validation flow:

```
Client request with JWT
    → JwtAuthenticationFilter (reads header, extracts token)
    → JwtService (validates token)
    → CustomUserDetailsService (loads user)
    → SecurityContext populated
    → Controller → business service
```

Responsibility boundaries to preserve:
- `JwtService`: create/validate tokens only.
- `JwtAuthenticationFilter`: read header → extract → validate → load user → set
  `SecurityContext`. Nothing more.
- `CustomUserDetailsService`: load user from DB, map to `UserDetails`. No JWT logic.
- `SecurityConfiguration`: wiring only (stateless session, JWT filter, password
  encoder, auth manager, CORS, authorization rules).
- `JwtAuthenticationEntryPoint`: 401 responses. `JwtAccessDeniedHandler`: 403
  responses.

Auth DTOs (`LoginRequest`, `LoginResponse`, `RegisterRequest`, `RefreshTokenRequest`)
live under `dto/auth/`, **not** under `security/` — they're API contracts, not
security infrastructure.

## Entity / Domain Conventions

- Use **`entity/`** for JPA-managed classes (`@Entity`), never `model/`. `model` is
  overloaded in this codebase's history (it could mean JPA entity, API request,
  view model, chat message) and becomes a dumping ground over time.
- Non-entity object types get their own home instead of a shared `model/` package:
  `dto/` for API contracts, `security/` for auth-related types, `ai/` or `chat/` for
  AI/chat-specific message types.
- Don't introduce a `domain/` package unless actually adopting full Domain-Driven
  Design patterns (aggregates, value objects, domain repositories) — for this
  layered Spring Boot app, `entity/` is sufficient and more conventional.

### Core entities

**Vehicle** — fleet record used for route assignment.
| Attribute | Description |
|---|---|
| `vehicle_id` (PK) | Unique identifier |
| `license_plate` | Vehicle registration number |
| `type` | Vehicle category/model |
| `capacity` | Maximum carrying capacity |
| `status` | Operational status |

**Route** — AI-generated delivery route; the aggregate root for route management.
| Attribute | Description |
|---|---|
| `route_id` (PK) | Unique identifier |
| `vehicle_id` (FK, **nullable**) | Assigned vehicle. Nullable because `RouteService.createRoute` and `assignVehicle(routeId, vehicleId)` are separate operations — a route is created first, then a vehicle is assigned. |
| `route_name` | Route name |
| `route_status` | Current status |
| `generated_time` | Generation timestamp |

**Job** — an individual delivery job assigned to a route.
| Attribute | Description |
|---|---|
| `job_id` (PK) | Unique identifier |
| `route_id` (FK, **nullable**) | Associated route. Nullable — jobs exist before any route does: `POST /api/jobs` (UC04) and the AI tool `createDeliveryJob` (FR02) both create unassigned jobs, and `PlanningService.generateOptimisedRoute` then consumes existing jobs to build a route. A `NOT NULL` FK here would make both flows impossible. |
| `job_name` | Delivery job name |
| `delivery_location` | Delivery destination (address string) |
| `latitude`, `longitude` | Nullable coordinates — not part of the ERD's original attribute set; exist only to back PostGIS. A job can exist with just `delivery_location` and no coordinates yet (e.g. right after AI-driven creation via `createDeliveryJob`); coordinates get attached later through the Jobs module UI. |
| `location` | Derived, DB-generated `geography(Point,4326)` from `latitude`/`longitude` (`GENERATED ALWAYS AS ... STORED`, GIST-indexed). Mapped read-only via `hibernate-spatial` (`org.locationtech.jts.geom.Point`, `insertable = false, updatable = false`). Never write to it directly — always write `latitude`/`longitude`. |
| `priority` | Delivery priority level |
| `status` | Current job status |

**Route distance is computed in `RouteSummaryServiceImpl`, not on the entity.** JPA entities never
issue queries — that would break the layering rule above. `RouteSummaryServiceImpl` injects
`RouteRepository` and calls a native query summing `ST_Distance` between consecutive stops'
`location` values in route order; the query must skip stops where `location` is null rather than
fail. Use `ST_Distance(a.location, b.location)` — the `geography` overload, metres, spheroidal.
`ST_DistanceSphere` takes `geometry` arguments only and fails with
`function st_distancesphere(geography, geography) does not exist`.

**RouteSummary** — reporting entity, one-to-one with `Route` (unique FK on
`route_id`). Keeps derived/statistical data out of the operational `Route` record.
| Attribute | Description |
|---|---|
| `summary_id` (PK) | Unique identifier |
| `route_id` (FK, UNIQUE) | Associated route |
| `total_distance` | Total planned travel distance |
| `estimated_duration` | Estimated travel time |
| `total_jobs` | Number of assigned jobs |
| `fuel_consumption` | Estimated fuel consumption |
| `generated_time` | Summary generation timestamp |

Relationships: `Vehicle 0..1—0..N Route`, `Route 0..1—0..N Job`, `Route 1—0..1 RouteSummary`.
Both FKs on the "many" side are nullable (see the entity tables above): a route may exist before
a vehicle is assigned, and a job may exist before it is planned onto any route.
Keep this normalization — never duplicate vehicle/job details onto `Route`, and
never store derived stats directly on `Route` (that's what `RouteSummary` is for).

## AI Layer Service Responsibilities

- **ChatService** — receives chat requests from `ChatController`, talks to the LLM,
  coordinates with `ToolCallingService` and `ConversationMemoryService`. Exposes two methods:
  `chat(request): ChatResponse` (blocking, used by tests and Bruno) and
  `chatStream(request): Flux<ServerSentEvent<String>>` (the one the real frontend calls — wraps
  `ChatClient.stream()` output as AI SDK UI Message Stream Protocol v1 frames, merging text-token
  frames with tool-call event frames from `LogisticsTools` so the AI Chat UI's tool-calling panel
  has real data to render).
  Three invariants hold for `chatStream`, each a hang or a malformed response if broken:
  `concat` the `start`/`text-start` and `text-end`/`finish`/`[DONE]` bookends around a `merge`
  of only the open-ended sources (`merge` interleaves by arrival, so a bookend inside it can be
  overtaken); complete the tool-event sink in `doFinally`/`doOnCancel` (a merged `Flux` never
  completes while a source stays open); and convert failures to an `error` frame with
  `onErrorResume` rather than throwing — once the first SSE byte is flushed the HTTP status is
  committed, so a thrown exception reaches the browser as a truncated 200, not a 500.
- **ToolCallingService** — an explicit, non-LLM invocation path over the same tool logic
  (`resolveTool(prompt)` / `executeTool(tool)`), used by tests and by a manual Bruno run of a
  tool without a live model. **It is not on the LLM's runtime path**: when the model calls a
  tool, Spring AI's auto-registered `ToolCallingAdvisor` invokes the `@Tool` method on
  `LogisticsTools` directly off the `ChatClient`, bypassing this service entirely. Do not put
  logic here expecting the model to reach it — it belongs in `LogisticsTools` or the business
  service beneath it.
- **ConversationMemoryService** — facade over Spring AI's `ChatMemory` /
  `JdbcChatMemoryRepository` (`spring-ai-starter-model-chat-memory-repository-jdbc`) — conversation
  history is persisted in Postgres, not held in an in-memory map, so any backend instance can
  serve any session.
  **The `ChatMemory.CONVERSATION_ID` advisor param is always server-derived:**
  `"<username>:<sessionId>"`, composed in `ChatServiceImpl` from the authenticated principal —
  never the raw `sessionId` off the request. Because memory is a shared, durable table, keying
  it on a client-supplied value would let any authenticated user read and append to another
  user's conversation. `ChatRequest.sessionId` is an opaque per-tab id with no security meaning.
- **PlanningService** — coordinates AI-assisted route optimization (an in-memory
  nearest-neighbour heuristic over job coordinates — no solver); delegates all
  persistence and route lifecycle work to `RouteService` (it must never call
  repositories directly).

Two distinct paths reach the business layer — preserve both when adding features:

```
LLM tool call (the real runtime path, FR02/UC02):
  ChatService → ChatClient → ToolCallingAdvisor (auto-registered) → LogisticsTools
      → JobService / VehicleService / PlanningService / RouteSummaryService → repositories

Explicit invocation (tests, Bruno, no live model):
  ToolCallingService → PlanningService → RouteService → repositories
```

In both cases `PlanningService → RouteService → repositories` holds: `PlanningService` never
touches a repository directly.

## General Coding Conventions

- Use constructor injection (`private final` fields assigned in the constructor) — no field
  injection, no Lombok.
- Business service implementations are `@Service` + `@Transactional`.
- Controllers depend only on service interfaces, never on `*ServiceImpl` or
  repositories directly.
- **Business services take and return DTOs, never JPA entities** — entities stay behind the
  service boundary so lazy loading and transaction scope never leak into controllers, tools, or
  the AI layer. Mapping happens in `mapper/`. (Cross-service calls may pass ids; helpers
  private to a single `*ServiceImpl` may use entities.)
- Repositories extend `JpaRepository`; one repository per aggregate/entity.
- Keep DTOs grouped by feature under `dto/<feature>/`, mirroring the domain areas
  (`auth`, `job`, `route`, `vehicle`, `chat`).
- New AI-invokable functions go in `tool/` as `@Tool`-annotated methods on `LogisticsTools`,
  registered on the `ChatClient` bean via `.defaultTools(logisticsTools)` in `OpenAiConfig` —
  **not** through `ToolCallingService` (see the two paths above). Keep `@Tool` methods thin;
  they call into existing business services rather than reimplementing logic. Spring AI
  auto-registers the `ToolCallingAdvisor` that drives the call/execute/respond loop — never
  hand-write it.
- Per-request state a `@Tool` method needs (the tool-call event sink `chatStream` merges into
  the SSE response, **and the authenticated principal**) is passed via Spring AI's
  `ToolContext`: declare a `ToolContext` parameter on the tool method and supply it per call
  with `.toolContext(...)`, reading the principal on the request thread before the model call.
  `LogisticsTools` is a singleton and tool execution during `ChatClient.stream()` runs on
  Reactor threads, so a field or a `ThreadLocal` will not work.
- **`SecurityUtils` / `SecurityContextHolder` must never be called from a `@Tool` method or
  anything reachable from one.** The security context is `ThreadLocal`-bound and is empty on the
  thread a tool runs on, so it returns `null` rather than failing loudly — a tool that authorises
  or audits off the current user silently acts unauthenticated. Take the principal from
  `ToolContext` instead.
- SSE responses (`/api/chat`) follow the AI SDK **UI Message Stream Protocol v1** (AI SDK 5): response header
  `x-vercel-ai-ui-message-stream: v1`, body lines `data: {"type":"...",...}`, terminated by
  `data: [DONE]`. Frame-building helpers live in `util/` (e.g. `AiSdkStreamEncoder`), not
  `mapper/` — this isn't entity↔DTO mapping. `CorsConfiguration` must expose that header
  (`Access-Control-Expose-Headers`) or the frontend can't read it cross-origin.
- OpenAI HTTP timeouts are set **in code, in `OpenAiConfig`**, on both a `RestClientCustomizer`
  and a `WebClientCustomizer` — Spring AI uses `RestClient` for blocking calls and
  `WebClient` for streaming, so configuring one leaves the other unbounded, and the UI only
  exercises the streaming path. There is **no** timeout property under
  `spring.ai.openai.chat.options`; that key holds model parameters only. The "≤10s AI response"
  NFR is measured as time to the first `text-delta` frame, not total completion.
- Geospatial columns: a scalar `latitude`/`longitude` pair is always the writable source of
  truth; a paired `geography(Point,4326)` column is DB-generated from them (never written to
  directly) and mapped read-only via `hibernate-spatial`. See `Job` under Core entities.
  Distance over `geography` uses `ST_Distance` (metres). `ST_DistanceSphere` and
  `ST_DistanceSpheroid` are geometry-only and would need an explicit `::geometry` cast.

## Tooling

- Build: **Maven** (wrapper committed at `logistics-assistant/mvnw`). Backend dependencies live in
  `logistics-assistant/pom.xml`.
- IDE: IntelliJ IDEA
- Version control: Git / GitHub
- API testing: Bruno. The collection lives in `api/` (`bruno.json`, one `.bru` file per request). Open that folder in Bruno and run it against the Local environment.
- UML modelling: Mermaid, Draw.io
- Migrations: Liquibase, formatted-SQL changelogs under
  `logistics-assistant/src/main/resources/db/changelog/changes/`, master file at
  `db/changelog/db.changelog-master.yaml`. Spring AI's JDBC chat-memory starter ships its own
  schema — don't hand-write a `spring_ai_chat_memory` migration. It does need
  `spring.ai.chat.memory.repository.jdbc.initialize-schema: always` in `application.yaml`: the
  default initialises embedded databases only, so on Postgres the table is otherwise never created.
- PostGIS integration tests (`@RequiresDocker`) skip when no Docker daemon is reachable. They run
  against `postgis/postgis`, not plain Postgres, because the schema runs `CREATE EXTENSION postgis`.

## Commands

Run from `logistics-assistant/` (on Windows, `mvnw.cmd` is the same wrapper):

| Purpose | Command |
|---|---|
| Compile + run all tests | `./mvnw test` |
| Clean build | `./mvnw clean verify` |
| Run the app | `./mvnw spring-boot:run` |
| Single test class | `./mvnw test -Dtest=com.wou.logistics_assistant.SomeTest` |

Local infrastructure: `docker compose -f docker/docker-compose.yml up -d` (PostGIS).
Frontend dev server: `npm run dev` from `app-ui/`.
