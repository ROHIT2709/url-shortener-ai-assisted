# Prompt Journal

A running log of every prompt given to the AI assistant during this project, along with the problem it raised, how it was resolved, and whether the user accepted or rejected the AI's suggestion.

---

## Entry 1 — 2026-07-07

**Prompt:** "journel or document every promt I am giving you in a file under doc folder, with three scenarios problem, resolution, remark wheather the user accepted or rejected the AI suggestions. document everything until explicitly mentioned to stop documenting."

**Problem:** The user wants a persistent audit trail of all prompts given to the AI, capturing the problem stated, the resolution provided, and whether the suggestion was accepted or rejected. No such journal existed in the project.

**Resolution:** Created `doc/prompt-journal.md` with a standard entry template (Prompt / Problem / Resolution / Remark). Every subsequent prompt will be appended here until the user explicitly says to stop documenting.

**Remark:** Accepted — journaling set up as requested.

---

## Entry 2 — 2026-07-07

**Prompt:** "alight in doc you will find a pdf an assistment for submission give me a laymen level walktrhough of this here is my understanding to this problem statement let me know if this is not the correct approch I see that its pointing to url shortner , so this shold have some form of encoding to shorten the url and save in the db , primary key should be the short id I would go with base 62 encoding here and the original url linking to short must me kept in db , accept the base 62 primary key which will 302 redirect to original let have the expiry enforced, also we need to validate whats coming in as url two layer url validation. Let me know if this design is accuratly matching the pdf requirement also let me know what can be added or anything I am missing."

**Problem:** User needed a layman-level walkthrough of the assignment PDF and a review of their proposed design (base62 encoding, short id as PK, 302 redirect, expiry, two-layer URL validation) against the PDF requirements.

**Resolution:** AI explained the PDF is two tests in one: (a) build a URL shortener with core APIs, analytics, and reliability features; (b) demonstrate disciplined AI-assisted engineering (decomposition, traceability, quality gates, three scenarios: greenfield/brownfield/ambiguous, docs, tests, final summary). Design review: corrected a key misconception — base62 encodes a generated ID, not the URL itself (encoding a URL cannot shorten it); confirmed short code as PK, DB mapping, 302 (with the analytics-vs-caching trade-off rationale vs 301), expiry, and two-layer validation (syntactic + SSRF/loop/safety checks). Identified gaps: analytics endpoint (click stats), reliability features (rate limiting, 404/410 semantics, health check, caching), duplicate-URL policy, staged commits to evidence the three scenarios, unit/integration tests, architecture/README/final-summary docs, and continued AI traceability.

**Remark:** Accepted — user approved the corrected design (base62 over generated ID, analytics, exception semantics) and asked to proceed with the boilerplate build.

---

## Entry 3 — 2026-07-07

**Prompt:** "go head with creating the boiler plate entity, DTOs, repository, service, controllers, exception handling, tests (I'll review and make changes ) once done will have our initila build."

**Problem:** Project was an empty Spring Boot 3.5 / Java 17 skeleton; needed the full initial implementation of the URL shortener per the agreed design.

**Resolution:** AI generated the initial build:
- `entity/UrlMapping` — numeric identity PK, unique indexed `shortCode`, `originalUrl`, `createdAt`, `expiresAt`, `clickCount`, `isExpired()` helper.
- `dto/` — `ShortenUrlRequest` (bean-validated: `@NotBlank`, `@Size(2048)`, optional `expiryDays` 1–365), `ShortenUrlResponse`, `UrlStatsResponse` (Java records).
- `repository/UrlMappingRepository` — Spring Data JPA, `findByShortCode`.
- `util/Base62` — encode/decode over generated ID (not the URL).
- `validation/UrlValidator` — layer 1 syntactic (well-formed URI, http/https only, host required, ≤2048 chars); layer 2 safety (blocks localhost, loopback, private/link-local/multicast IP literals — SSRF guard; no DNS resolution to stay side-effect free).
- `service/UrlShortenerService` — shorten (save → encode ID → set code, default 30-day expiry, configurable), resolve (404 unknown / 410 expired / click increment), stats.
- `controller/UrlController` (`POST /api/urls` → 201, `GET /api/urls/{code}/stats`) and `RedirectController` (`GET /{code}` → 302 with regex path guard so `/api/**` and `/actuator/**` aren't swallowed).
- `exception/` — `UrlNotFoundException` (404), `UrlExpiredException` (410), `InvalidUrlException` (400), `ApiError` body, `GlobalExceptionHandler` (@RestControllerAdvice, also handles bean-validation 400s and a generic 500).
- `application.properties` — H2 in-memory datasource, `app.base-url`, `app.default-expiry-days=30`.
- Tests — `Base62Test` (12), `UrlValidatorTest` (22), `UrlShortenerServiceTest` (8, Mockito), `UrlShortenerIntegrationTest` (7, @SpringBootTest + MockMvc end-to-end: create→redirect→stats, 404/400 paths).

Build issue found and fixed during verification: system JDK was Corretto 11 (`release version 17 not supported`); ran Maven with IntelliJ-managed Corretto 17 from `~/.jdks/corretto-17.0.19`. Result: **50 tests run, 0 failures**.

**Remark:** Accepted with corrections — user reviewed, ran the app locally, and caught a defect: unmapped routes returned 500 instead of 404 (see Entry 4). User also adjusted `server.port`/`app.base-url` to 8081.

---

## Entry 4 — 2026-07-07

**Prompt:** "I see in controller dont neet a get , however when I complied and ran this on localhost I am getting 500 http status this should point to methodnotfound exeception as there is no java method linked to it, also for testing the inmemory database add the base url path \h2"

**Problem:** (1) Requests to paths with no mapped handler returned HTTP 500 instead of a proper 404/405 — the user correctly identified that "no java method linked" should not surface as a server error. (2) The H2 console needed to be reachable at `/h2` for inspecting the in-memory database.

**Resolution:** Root cause: the AI-generated catch-all `@ExceptionHandler(Exception.class)` in `GlobalExceptionHandler` was swallowing Spring MVC's routing exceptions (`NoResourceFoundException` → 404, `HttpRequestMethodNotSupportedException` → 405), re-labelling them as 500. Fixed by checking whether the caught exception implements Spring's `ErrorResponse` interface and honouring its status code; only genuinely unexpected exceptions now return 500. Added `spring.h2.console.path=/h2` (console at `http://localhost:8081/h2`, JDBC URL `jdbc:h2:mem:urlshortener`, user `sa`). Added two regression tests: unmapped path → 404, wrong HTTP method → 405. Full suite: 52 tests, 0 failures.

**Remark:** Accepted — defect found by user during manual review/run (a good example of human oversight catching an AI-generated bug); fix applied and regression-tested.

---

## Entry 5 — 2026-07-07, ~12:30 IST

**Prompt:** "read through doc/prompt-journal.md file intellij closed in between catch-up." followed by "add postgres dependecy"

**Problem:** IntelliJ had closed mid-session; the user needed a catch-up from the journal, then asked to add the PostgreSQL driver dependency for the upcoming containerized setup.

**Resolution:** AI summarized the four journal entries to restore context. On inspecting `pom.xml`, found `org.postgresql:postgresql` (runtime scope) already present — likely selected at Spring Initializr time — so no change was needed.

**Remark:** Accepted — no code change required; dependency was already in place.

---

## Entry 6 — 2026-07-07, ~12:35 IST

**Prompt:** "Generate a multi-stage Dockerfile, .dockerignore, and docker-compose.yml with PostgreSQL, using Docker and Spring Boot best practices. Ensure environment variables are used instead of hardcoded values"

**Problem:** The app only ran locally against in-memory H2; it needed containerization with a real PostgreSQL database, following best practices (multi-stage build, small runtime image, non-root user, layer caching) with all credentials/config externalized to environment variables rather than hardcoded.

**Resolution:** AI generated four files in the Maven module root:
- `Dockerfile` — multi-stage: Maven build stage with dependency-layer caching (`dependency:go-offline` against the POM alone), then Spring Boot layertools extraction (`-Djarmode=layertools`) so dependencies/loader/application land in separate image layers; runtime stage on `eclipse-temurin:17-jre-alpine`, non-root `spring` user, container-aware heap (`-XX:MaxRAMPercentage=75.0`), exec-form `JarLauncher` entrypoint.
- `.dockerignore` — excludes `target/`, VCS/IDE files, docs, and `.env` (secrets never enter the build context).
- `docker-compose.yml` — `postgres:16-alpine` with named volume, `pg_isready` healthcheck, and the app service gated on `condition: service_healthy`; every credential and setting flows from `${VAR:-default}` interpolation, with `POSTGRES_PASSWORD` required (`:?` — compose refuses to start without it). Spring config overridden via relaxed-binding env vars (`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_DRIVER_CLASS_NAME=org.postgresql.Driver` — necessary because `application.properties` hardcodes the H2 driver, `SPRING_H2_CONSOLE_ENABLED=false`).
- `.env.example` — documented template; real `.env` stays git-ignored.

Noted config drift for the user: `application.properties` has `server.port=8080` but `app.base-url` points at `:8081` — the compose setup standardizes on 8080 via env vars.

**Remark:** Initially rejected — not on technical grounds, but because the AI wrote the files before journaling the prompt, violating the Entry 1 documentation rule (see Entry 8). Files re-applied after the journal was brought up to date.

---

## Entry 7 — 2026-07-07, 12:36 IST

**Prompt:** "alight are you journeling the time stamp as well"

**Problem:** Journal entries recorded only the date (2026-07-07), not the time of day — insufficient granularity for an audit trail with many same-day entries.

**Resolution:** Confirmed timestamps were missing. From Entry 5 onward every entry header carries a time (IST); entries 5–6 are marked approximate (~) since they were journaled retroactively. Entries 1–4 remain date-only because their exact times were not recorded.

**Remark:** Accepted — timestamps added going forward.

---

## Entry 8 — 2026-07-07, 12:39 IST

**Prompt:** "adhere to all the conditions in prompt-journal.md file in doc folder"

**Problem:** The AI generated the Docker artifacts (Entry 6) without first journaling the prompts from that exchange — drifting from the Entry 1 rule that every prompt be documented. The user rejected the file writes and reasserted the rule.

**Resolution:** Journal brought current (entries 5–8 added with timestamps), the persistent memory rule strengthened to journal each prompt *as part of handling it* rather than afterwards, and only then were the Docker files from Entry 6 applied.

**Remark:** Accepted — process corrected; journaling now precedes/accompanies the work itself.

---

## Entry 9 — 2026-07-07, 12:52 IST

**Prompt:** "isntalled docker in my machine create a image and try to run the image, if the build fail daignose and fix show me stats of the failures."

**Problem:** Docker was freshly installed; the image from Entry 6's Dockerfile had never been built or run. Needed: build the image, run the stack, and if anything fails, diagnose, fix, and report failure stats.

**Resolution:** First attempt hit two environment issues before any build could start: (1) the fresh Docker install wasn't on the shell's PATH (fixed by reloading PATH from the registry — `docker` CLI 29.6.1 found); (2) the Docker engine wasn't running (Docker Desktop launched, engine took time to initialize). Continued in Entry 10.

**Remark:** Superseded by the retry in Entry 10.

---

## Entry 10 — 2026-07-07, 12:58 IST

**Prompt:** "retry creating image and running the instance"

**Problem:** Continuation of Entry 9 — the first attempt stalled while waiting for the freshly installed Docker Desktop engine to finish initializing. User asked to retry the image build and run the containers.

**Resolution:** (User also sent "sorry good" acknowledging the interim status — logged here rather than as a separate entry.) The image build never got to start; every failure was environmental. Diagnosis chain (5 failures, all root-caused):
1. `docker` not recognized in shell → session PATH predated the Docker install → fixed by reloading PATH from the registry (CLI 29.6.1 found).
2. `docker info` → cannot connect to `//./pipe/docker_engine` → Docker Desktop not running → launched it.
3. Engine pipe returned HTTP 500 indefinitely → Desktop up but Linux engine never initialized → investigated backend.
4. **Root cause:** `wsl -l -v` revealed WSL is not installed — Docker Desktop on Windows 11 Home requires the WSL2 backend. Fixed via elevated `wsl --install --no-distribution` (UAC approved by user).
5. `wsl --status` then reported "virtualization is not enabled" → checked firmware: `HyperVisorPresent=True`, so BIOS virtualization is fine; real blocker is `CBS RebootPending=True` — the freshly enabled Virtual Machine Platform component needs a Windows restart.

Also done while waiting: `docker compose config` validated (syntax OK) and a git-ignored `.env` with local dev credentials was created for the compose stack. **Next step: user reboots Windows, then the build/run proceeds (Entry 11).**

**Remark:** (pending — awaiting Windows reboot; build/run resumes after restart)

---

## Entry 11 — 2026-07-07, 13:07 IST

**Prompt:** "I dont see any env varibale added to applciation properrties for postgres"

**Problem:** `application.properties` still showed only hardcoded H2 settings with no visible Postgres/env-var wiring. The Entry 6 design relied on Spring Boot's *relaxed binding* (compose exports `SPRING_DATASOURCE_URL` etc., which silently override `spring.datasource.*`) — functional, but invisible to anyone reading the properties file, and the H2 driver override lived only in docker-compose.yml.

**Resolution:** Made the env-var wiring explicit with `${VAR:default}` placeholders in `application.properties` — defaults preserve the local H2 dev experience (run with zero env vars set), while Docker/prod injects overrides: `DB_URL`, `DB_DRIVER`, `DB_USERNAME`, `DB_PASSWORD`, `DDL_AUTO`, `H2_CONSOLE_ENABLED`, `SERVER_PORT`, `APP_BASE_URL`, `APP_DEFAULT_EXPIRY_DAYS`. `docker-compose.yml` updated to set these named vars instead of the implicit `SPRING_*` relaxed-binding ones. Side fix: `app.base-url` now defaults to `http://localhost:${server.port}` (nested placeholder), eliminating the 8080-vs-8081 drift flagged in Entry 6. Full test suite re-run to confirm property resolution still loads the Spring context.

**Remark:** Accepted — user explicitly wanted the env variables visible in `application.properties`; the implicit-override approach was replaced. Verified: full suite re-run after the change — 52 tests, 0 failures.

---

## Entry 12 — 2026-07-07, 13:20 IST

**Prompt:** "looks like virtulisation is not supported in my system any hosted site where i can clone and run this"

**Problem:** After the reboot, WSL2 still couldn't start — the user concluded local virtualization isn't usable, so Docker can't run on this machine. They asked for a hosted service where the repo can be cloned and the containerized stack run instead.

**Resolution:** Recommended (1) **GitHub Codespaces** as the primary option — free monthly quota, no credit card, Docker-in-Docker works, so the existing `docker-compose.yml` runs unmodified and forwarded ports give a shareable demo URL; (2) **Render** free tier for an always-on public deployment — builds the Dockerfile straight from GitHub with a managed Postgres, wiring credentials through the same `DB_URL`/`DB_USERNAME`/`DB_PASSWORD` env vars introduced in Entry 11; (3) Railway noted as an alternative (trial credit only). Also noted: the i5-1135G7 does support VT-x, so BIOS-level enablement remains a possible local fix. Prerequisite flagged: the repo has a GitHub remote (`ROHIT2709/url-shortener-ai-assisted`) but zero commits — code must be committed and pushed before any hosted option can clone it.

**Remark:** (pending — user to pick a platform; offered to commit & push as the first step)

---

## Entry 13 — 2026-07-07, 13:23 IST

**Prompt:** "I have already commited the latest in main branch , do a mvn test get all the test cases done and in loop keep fixing for error I am doing a parallel testing in postman also add swagger api for documentation and give an easy access path"

**Problem:** (1) Add Swagger/OpenAPI documentation with an easy-to-remember access path; (2) run `mvn test` and keep fixing in a loop until all tests pass, while the user tests in parallel via Postman. Design catch identified up front: the redirect catch-all `GET /{shortCode:[0-9a-zA-Z]{1,16}}` would swallow a short docs path like `/docs` and return 404 — the shortCode pattern needed reserved words.

**Resolution:** Added `springdoc-openapi-starter-webmvc-ui` to the POM; `springdoc.swagger-ui.path=/docs` as the easy access path (`/v3/api-docs` for the raw spec, default `/swagger-ui/index.html` still available); `OpenApiConfig` bean with API title/description/version; `@Tag`/`@Operation`/`@ApiResponse` annotations on both controllers so the UI documents the 201/400/404/410 semantics; RedirectController regex hardened with a negative lookahead `(?!(?:docs|h2|api|actuator)$)` reserving those words from shortCode matching; new `ApiDocsIntegrationTest` covering `/docs` redirect + `/v3/api-docs` content (regression-guards the reserved-word fix). Test loop run until green — final outcome recorded below after the loop.

**Remark:** Accepted — implemented as designed. **Test-loop stats: converged on run 1 — 55 tests (52 existing + 3 new), 0 failures, 0 errors, 0 fixes needed.** The user's live IntelliJ instance (port 8080) was left untouched for parallel Postman testing; a Maven reload + app restart is needed there to pick up the Swagger UI. Git discrepancy flagged: the user believed the latest was committed to `main`, but the working copy's branch `master` still has zero commits (files only staged) — raised for the user to reconcile before any hosted deploy (Entry 12 prerequisite).

---

## Entry 14 — 2026-07-07, 13:41 IST

**Prompt:** "okay testes passed , lets create a simple CI for github actions create the yml file , notify once done" (user also confirmed the Entry 13 tests passed on their side)

**Problem:** No CI existed — nothing verifies the build or the 55-test suite on push/PR once the repo lands on GitHub (a stated assignment expectation and a prerequisite for confident hosted deploys per Entry 12).

**Resolution:** Created `.github/workflows/ci.yml` at the repo root (workflows must live at the root; the Maven module is nested, handled via a default `working-directory: url-shortner`). Simple single-job pipeline: triggers on push/PR to `main`/`master`, checkout → Temurin JDK 17 with built-in Maven dependency caching → `chmod +x mvnw` (executable bit is unreliable when committed from Windows) → `./mvnw -B verify` → surefire reports uploaded as an artifact only when tests fail. Desktop notification sent on completion as requested.

**Remark:** Superseded by Entry 15 — the file had been created in the wrong repo (see below).

---

## Entry 15 — 2026-07-07, 13:46 IST

**Prompt:** "create the .githib yml file in D:\Projects\url-shortner\url-shortner\src\main\java\com\rohit\url_shortner this path where my remote origin is"

**Problem:** Entry 14's workflow was created at `D:\Projects\url-shortner\.github\` — but investigation triggered by this prompt revealed **two nested git repos**: the outer folder (branch `master`, zero commits — the source of the Entry 13 "nothing committed" flag) and the Maven module `D:\Projects\url-shortner\url-shortner` (branch `main`, 3 commits, pushed to `ROHIT2709/url-shortener-ai-assisted`). The module is the real repo root — the user's "committed to main" claim was correct all along. The package path the user quoted resolves to that repo; workflows belong at its root, not in the Java package folder.

**Resolution:** Recreated the workflow at `url-shortner/.github/workflows/ci.yml` (the pushed repo's root) with paths corrected for the module-rooted layout: no `working-directory` indirection, surefire artifact path `target/surefire-reports/`, trigger branches `[main]`. Deleted the misplaced copy from the outer folder. Flagged to the user: (a) the `doc/` folder — including this journal and the assignment PDF — lives **outside** the pushed repo and won't reach GitHub as-is; (b) the Entry 13 Swagger changes are still uncommitted in the real repo.

**Remark:** Accepted — CI recreated in the correct repo; goes live on the next push of `main`.

---

## Entry 16 — 2026-07-07, 13:52 IST

**Prompt:** "move the doc to the url-shortner path and create the CI yml file"

**Problem:** The `doc/` folder (this journal + the assignment PDF) lived outside the pushed repo (Entry 15 flag a), so the assignment's documentation trail would never reach GitHub.

**Resolution:** Moved `D:\Projects\url-shortner\doc` → `D:\Projects\url-shortner\url-shortner\doc`; git now shows it as untracked `doc/` ready to commit. All future journaling continues at the new path (persistent memory rule updated accordingly). The CI yml needed no new work — `.github/workflows/ci.yml` was already in place from Entry 15; verified present. Repo now stages everything for one commit: Swagger changes (Entry 13), CI workflow (Entry 15), and docs (this entry).

**Remark:** Accepted — doc folder now part of the repo; CI file confirmed in place.

---
