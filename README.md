# LabAssist — Lab Results Smart Assistant

A small hospital system that ingests test results from lab devices, lets a doctor review them,
flags abnormal values, and provides an **AI-assisted preliminary interpretation** — with
password login, encryption of patient data, and an audit trail.

> ⚕️ All patient data in this project is **synthetic**, produced by a mock device. The AI
> commentary is a **preliminary, non-diagnostic** decision-support aid and always recommends
> physician review.

![Reports dashboard](docs/screenshots/02-reports.png)

---

## System at a glance

```
┌──────────────────┐   HTTP poll    ┌─────────────────────┐   REST + JWT   ┌────────────────┐
│  mock-lab-device │ ─────────────▶ │      backend        │ ◀───────────── │    frontend    │
│  (Node/Express)  │   JSON results │   (Spring Boot)     │                │ (React + MUI)  │
│  many scenarios  │                │  validate · flag ·  │                │  login · list  │
└──────────────────┘                │  encrypt · audit    │                │  detail · AI   │
                                     └─────────┬───────────┘                └────────────────┘
                                               │  prompt (no PII)
                                     ┌─────────▼───────────┐     ┌──────────────┐
                                     │  Ollama llama3.2:3b  │     │  PostgreSQL  │
                                     └──────────────────────┘     └──────────────┘
```

| Component | Stack | Responsibility |
|---|---|---|
| `mock-lab-device/` | Node/Express + TypeScript | Simulates a lab analyzer; emits JSON results across many scenarios |
| `backend/` | Spring Boot 3.5 (Java 17, Maven) | Polls, validates, flags, encrypts, stores, serves the REST API, calls the LLM, audits |
| `frontend/` | React 18 + Vite + TypeScript + MUI | Doctor login, result list with abnormal highlighting, detail view, AI interpretation |
| `db` | PostgreSQL 16 + Flyway | Persistence + reproducible schema/seed |
| `llm` | Ollama (llama3.2:3b) | Local, CPU-friendly preliminary interpretation |

More detail in [`docs/architecture.md`](docs/architecture.md) and the step-by-step
[`docs/usage-guide.md`](docs/usage-guide.md).

---

## Quick start (Docker — one command)

> Prerequisites: Docker + Docker Compose. The first run downloads the **~2 GB** `llama3.2:3b`
> model into a named volume (subsequent runs reuse it).

```bash
cp .env.example .env          # secrets for JWT signing + PII encryption (already filled with dev values)
docker compose up --build     # postgres · ollama (+model pull) · mock · backend · frontend
```

Then open **<http://localhost:5173>** and sign in with a seeded account:

| Role | Username | Password |
|---|---|---|
| Doctor | `doctor` | `Doctor123!` |
| Admin (also sees the audit log) | `admin` | `Admin123!` |

The backend begins polling the mock device immediately, so the report list fills within seconds.
Interactive API docs (Swagger UI): **<http://localhost:8080/swagger-ui.html>**.

> ⏱️ The LLM runs on CPU here (no GPU), so the **first** interpretation for a report takes
> ~30–60 s; the result is cached, so re-opening it is instant.

---

## Local development workflow

Run the infrastructure in Docker and the apps natively for fast iteration:

```bash
# 1) Infra
docker compose up -d postgres ollama
docker compose exec ollama ollama pull llama3.2:3b   # once

# 2) Mock device  (http://localhost:9090)
cd mock-lab-device && npm install && npm run dev

# 3) Backend      (http://localhost:8080)
cd backend && ./mvnw spring-boot:run

# 4) Frontend     (http://localhost:5173)
cd frontend && npm install && npm run dev
```

Configuration is environment-driven with sensible local defaults (see
`backend/src/main/resources/application.yml` and `.env.example`).

Run the backend tests (unit + Testcontainers — needs Docker):

```bash
cd backend && ./mvnw test
```

---

## REST API (overview)

All endpoints except login and Swagger require `Authorization: Bearer <jwt>`.

| Method | Path | Description |
|---|---|---|
| POST | `/api/auth/login` | Authenticate, receive a JWT |
| GET | `/api/auth/me` | Current user |
| GET | `/api/lab-reports` | Paged list; filters: `abnormalOnly`, `status`, `q` (report id) |
| GET | `/api/lab-reports/{id}` | Report detail with analytes + flags (PII decrypted) |
| POST | `/api/lab-reports/{id}/interpretation` | Generate (or return cached) AI interpretation; `?refresh=true` to regenerate |
| GET | `/api/lab-reports/{id}/interpretation` | Latest stored interpretation (204 if none) |
| GET | `/api/audit` | Audit trail — **ADMIN only** |

---

## Design decisions & rationale

**Stack**

- **Spring Boot 3.5 (not 4.0).** Initializr defaulted to the brand-new Boot 4 / Spring 7 line;
  I pinned to the mature 3.5 LTS-aligned release for stability, broad library compatibility
  (springdoc, jjwt) and APIs I can confidently defend. Same reasoning drove **React 18 + MUI 6 +
  X-Data-Grid 7** instead of the just-released MUI 9.
- **Java 17 + Maven wrapper**, **Vite + TypeScript**, **PostgreSQL + Flyway** — standard,
  reproducible, enterprise-familiar. Flyway owns the schema; JPA runs in `validate` mode.
- **Ollama + `llama3.2:3b`.** Local, free, fully reproducible, and small enough to run on a
  CPU-only box (~2 GB). Trade-off: a 3B model favors latency/footprint over depth — acceptable
  for *preliminary* commentary. Swappable via `OLLAMA_MODEL`.
- **Node/Express mock** as a *separate* service — it represents an external analyzer the backend
  polls over HTTP, so a distinct process (and stack) keeps that boundary honest and makes
  scenario authoring easy.

**Business logic & AI**

- **The backend owns abnormality flagging.** It recomputes every flag from its **own seeded
  reference-range catalog** (including sex-specific ranges, e.g. hemoglobin) rather than trusting
  the ranges a device sends. Don't trust external input for clinical logic.
- **Hybrid analysis.** A deterministic rule engine (`AbnormalityEvaluator`) decides what is
  `NORMAL/LOW/HIGH/CRITICAL_*`; the LLM only *narrates* a preliminary interpretation grounded in
  those pre-computed flags. The system is never blindly dependent on the model, and flags are
  reproducible and testable.
- **Privacy by design — no PII to the LLM.** The prompt contains only age, sex, analyte values,
  reference ranges and flags. Patient **name and MRN are never sent to the model** (enforced and
  unit-tested).
- **Safety framing.** The system prompt forbids definitive diagnosis, leads with critical values,
  and always ends recommending physician review. The UI shows a non-diagnostic disclaimer.

**Resilience & correctness (ingestion)**

- Scheduled poll with an in-memory cursor; **idempotent on `external_id`** (duplicates skipped).
- **Each message is validated and persisted in its own transaction**, so one bad message never
  aborts the batch. Malformed payloads → `REJECTED` (raw payload + reason kept); missing values →
  `PARTIAL`; otherwise `VALIDATED`.
- Device errors / timeouts / empty batches are logged and retried next cycle with the cursor
  untouched — no data loss. The mock deliberately injects all of these.

**Security, encryption & audit**

- **JWT (HS256) + BCrypt**, stateless sessions, CORS limited to the SPA origin, role-based access
  (`DOCTOR` / `ADMIN`), consistent JSON `401/403/404` responses.
- **Field-level encryption of PII at rest.** Patient name and MRN are encrypted with
  **AES-256-GCM** (authenticated) via a JPA `AttributeConverter`; ciphertext in the DB, transparent
  decryption only for authorized API responses. Verified at-rest in an integration test.
- **Audit trail in a dedicated `audit_log` table** (the brief's "logging system"): login
  success/failure, ingestion polls, report views and LLM requests — with an admin viewer. Audit
  writes use `REQUIRES_NEW` so they persist independently of the business transaction.

**LLM endpoint**

- Persists **every** attempt (`SUCCESS/FAILURE/TIMEOUT`) for auditability, **caches** the latest
  success (regenerate with `?refresh=true`), is **not** wrapped in a DB transaction during the
  slow call, and maps runtime failures to a clear **503**.

---

## What I deliberately did not do (and why)

- **Streaming LLM responses** — would improve perceived latency; non-streaming kept the contract
  simple. A natural next step.
- **Refresh tokens / revocation** — a single short-lived access token is enough for this scope.
- **Searchable encrypted fields** — because name/MRN are encrypted at rest, free-text search is by
  report id only. A blind index would enable encrypted-field search at the cost of complexity.
- **TLS/HTTPS termination** — a reverse proxy / ingress concern in production, out of scope locally.
- **Frontend test suite** — testing effort was concentrated on the backend (where the business
  logic lives); only high-value paths are covered.
- **Real device protocol (HL7 v2 / FHIR)** — the mock speaks JSON; a production integration would
  parse the real wire format.
- **Queue-based / horizontally-scaled ingestion (e.g. Kafka)** — a single scheduled poller is
  sufficient at this scale.

---

## Testing

- **Unit (JUnit 5 + Mockito):** abnormality flagging, AES-GCM encryption, JWT, the de-identified
  prompt (asserts no PII leaks), reference-range selection.
- **Integration (Testcontainers + real PostgreSQL):** the full ingestion path
  (validate → flag → encrypt → persist, incl. critical / partial / malformed / duplicate and
  PII-ciphertext-at-rest), and the API/security/LLM endpoints over MockMvc (Ollama mocked).

`cd backend && ./mvnw test` → 32 tests, all green.

---

## Repository layout

```
labassist/
├── mock-lab-device/   # Node/Express/TS — lab device simulator (scenarios + chaos)
├── backend/           # Spring Boot — ingestion, REST API, auth, crypto, LLM, audit
├── frontend/          # React + Vite + MUI — doctor dashboard
├── docs/              # architecture, usage guide, screenshots
├── docker-compose.yml # full stack, one command
└── .env.example       # config template
```
