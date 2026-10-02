# Delivery Backlog

Everything planned for this demo, done and to do, in delivery order. Tick items off as they are merged.

Each user story is also a GitHub issue; the issue number is shown where one exists. The design is in [`design-proposal.md`](design-proposal.md) (§13 explains the sprints), and review findings are in [`review-log.md`](review-log.md).

**Legend:** `[x]` done · `[ ]` to do · **R-0xx** = review finding fixed by that item

---

## Foundation

- [x] Design document: scope, architecture, data model, API, AI boundary, security, testing
- [x] Backend scaffold: Spring Boot 3.5, Java 17, Maven wrapper
- [x] Frontend scaffold: React 19, strict TypeScript, Vite, Vitest
- [x] CI: GitHub Actions for backend and frontend
- [x] Dependabot, with major-version rules for Spring Boot, TypeScript, Node types and jsdom
- [x] Issue template (user story) and pull request template
- [x] Agile delivery plan and Definition of Done
- [x] Review log, with code review in the Definition of Done

## Sprint 1 — Walking skeleton

*Goal: a reviewer can take a purchase order from upload to confirmation.*

- [x] #6 Database schema and module structure
  - [x] Review fixes: R-001, R-002, R-004
  - [x] Unit tests for review findings, and revert check of every fix (R-011)
- [x] #7 Upload a purchase order: API with signature-based file checks
  - [x] Review fixes: R-007, R-008, R-009
  - [x] Upload screen (delivered with #8)
- [x] #8 See my uploaded documents: paginated list API and documents screen
  - [x] Review fixes: R-012 to R-016
- [x] #9 Extract order data with AI: AI module, mock provider, status rules, extraction screen
  - [x] Review fix: R-018
- [x] #10 Review the extracted order next to the document
  - [x] Endpoint that serves the original file (inline, detected type, `nosniff`, restrictive CSP)
  - [x] Side-by-side screen: document viewer and extracted data
  - [x] Review fixes: R-022, R-023
- [x] #11 Correct the extracted data
  - [x] `PUT /api/documents/{id}/extraction` for header and existing lines, with version check and per-field errors
  - [x] Editable form; status becomes In review; `EXTRACTION_EDITED` audit event
  - [x] Review fixes: R-026, R-027, R-028
- [x] #12 Confirm the extracted order
  - [x] `POST /api/documents/{id}/review/confirm` with the version the reviewer saw (R-032); confirmation dialog; read-only afterwards
  - [x] "Confirmed by … on …" shown; `EXTRACTION_CONFIRMED` audit event
  - [x] Test for R-027 (no edits once confirmed)
- [ ] **Sprint close**
  - [ ] Merge `develop` into `main` (closes the sprint's issues)
  - [ ] README: what it is, how to run it, first screenshots
  - [ ] Short demo recording of the full workflow

## Sprint 2 — Trust the data

*Goal: reviewers can see what to check and fix it.*

- [ ] Validator for AI output: structure, field rules, at least one line (R-021)
- [ ] Arithmetic checks: line totals, subtotal and total, with stated vs calculated values
- [ ] Confirmation blocked while arithmetic errors remain
- [ ] Validation flags shown on screen
- [ ] Line-items grid: add, remove and edit lines
- [ ] "AI extracted" / "Edited" badges from field provenance
- [ ] Typed AI snapshot that keeps decimal precision (R-005)
- [x] Optimistic locking surfaced in the UI ("changed by someone else"), delivered with #11
- [ ] Test for a version clash between two open transactions (R-028)
- [ ] Application validation before the database, so CHECK constraints are only a last line of defence (R-003)
- [ ] One mock sample with a deliberate arithmetic error

## Sprint 3 — Secure it

*Goal: only the right people can do the right things.*

- [ ] Session login with CSRF protection; login and logout screens
- [ ] Roles: REVIEWER (does the work), ADMIN (views, cannot edit or confirm)
- [ ] Demo users created from environment variables
- [ ] Current user taken from the security context instead of the demo user
- [ ] Security exceptions return 401 and 403, not 500 (R-006)
- [ ] Security headers; CORS for configured development origins only
- [ ] Tests for every authorisation rule

## Sprint 4 — Accountability

*Goal: every change can be traced.*

- [ ] Field and line diffs in `EXTRACTION_EDITED` events
- [ ] Audit endpoint and audit timeline screen
- [ ] Dashboard: status filter (pagination is done)
- [ ] Duplicate-upload warning using the stored SHA-256

## Sprint 5 — Real AI

*Goal: real documents are read by a real model.*

- [ ] Gemini provider (Google Gen AI SDK, structured output, refusal and rate-limit handling)
- [ ] Provider contract tests with WireMock, run against both mock and Gemini
- [ ] Asynchronous extraction with polling; `POST /process` returns 202
- [ ] Stuck-job sweeper for documents left in Processing (R-019)
- [ ] Improvement case study: measure synchronous vs asynchronous extraction, before and after

## Sprint 6 — Ship it

*Goal: anyone can run it and understand it.*

- [ ] Dockerfiles for backend and frontend (nginx); `docker compose up` runs everything
- [ ] Synthetic sample purchase orders (PDF and photo) whose content matches the mock's output (R-025)
- [ ] Check the review screen in Safari and Firefox (R-024)
- [ ] Accessibility pass: inert background behind dialogs, keyboard walkthrough (R-033)
- [ ] OpenAPI / Swagger UI
- [ ] Structured JSON logging with correlation IDs
- [ ] Test coverage report (JaCoCo)
- [ ] Review documents, written from the review log: architecture, database, security, API, testing and QA, performance
- [ ] Final README, screenshots and demo video

## Open review findings

Each open finding is scheduled above.

| Finding | Summary | Scheduled |
|---|---|---|
| R-003 | MySQL CHECK violations aren't classified as integrity errors | Sprint 2 |
| R-005 | AI snapshot numbers lose decimal precision | Sprint 2 |
| R-006 | Catch-all error handler would hide authorisation failures | Sprint 3 |
| R-019 | A document can get stuck in Processing | Sprint 5 |
| R-021 | AI output isn't validated before it is saved | Sprint 2 |

## Technical improvements (not tied to a sprint)

- [ ] Split unit and integration tests (`*IT` with Maven Failsafe), so `./mvnw test` runs without Docker
- [ ] Remove the jsdom 29 pin once Vitest supports jsdom 30 (R-012)
