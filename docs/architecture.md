# Architecture

## Data flow

```
                         (1) HTTP poll  ?since=<cursor>
  ┌──────────────────┐  ───────────────────────────────▶  ┌───────────────────────────┐
  │  mock-lab-device │                                     │          backend          │
  │  (Node/Express)  │  ◀───────────────────────────────  │       (Spring Boot)       │
  │                  │     JSON batch { cursor, results }  │                           │
  │  scenarios:      │                                     │  LabResultPoller (@Sched) │
  │   normal         │                                     │       │                   │
  │   abnormal       │                                     │       ▼                   │
  │   critical       │                                     │  MessageIngestor          │
  │   partial        │                                     │   validate → flag →       │
  │   malformed      │                                     │   encrypt PII → persist   │
  │   + duplicates   │                                     │       │                   │
  │   + 500/latency  │                                     │       ▼                   │
  └──────────────────┘                                     │  PostgreSQL (Flyway)      │
                                                           │       ▲                   │
  ┌──────────────────┐     REST + JWT (Bearer)             │       │                   │
  │     frontend     │  ◀───────────────────────────────▶│  REST controllers         │
  │  (React + MUI)   │     login · list · detail · AI      │   auth · reports · audit  │
  └──────────────────┘                                     │   llm                     │
                                                           │       │ prompt (no PII)   │
                                                           │       ▼                   │
                                                           │  Ollama (qwen2.5:3b)      │
                                                           └───────────────────────────┘
```

## Backend layering (`com.labassist`)

Package-by-feature with a clean layering inside each feature:

```
config/      typed @ConfigurationProperties, SecurityConfig (JWT/CORS/roles)
common/      exception/ , web/ (ApiError, GlobalExceptionHandler, PagedResponse, RequestUtils)
crypto/      EncryptionService (AES-256-GCM) + PiiAttributeConverter (JPA @Convert)
security/    AppUser, JwtService, JwtAuthenticationFilter, AuthService, AuthController, seed
ingestion/   client/ (LabDeviceClient, device DTOs), MessageIngestor, IngestionService, poller
labresult/   LabReport + TestResult entities, repository, service, controller, DTOs,
             reference/ (ReferenceRange, ReferenceRangeCatalog, AbnormalityEvaluator)
audit/       AuditLog, AuditService, AuditController
llm/         OllamaClient, PromptBuilder, LlmInterpretationService, LlmController
```

## Data model

| Table | Purpose | Notes |
|---|---|---|
| `app_user` | Doctors/admins | BCrypt `password_hash`, role enum |
| `reference_range` | Canonical analyte ranges | seeded (Flyway V2); sex/age aware |
| `lab_report` | One report per device message | `patient_name`/`patient_mrn` **encrypted**; `status`, `overall_abnormal`, `abnormal_count`, `critical_count`, `raw_payload` (jsonb), `rejection_reason` |
| `test_result` | One analyte measurement | `value`, `ref_low/high`, computed `flag` |
| `llm_interpretation` | Stored AI outputs | `model`, `status`, `response_text`, `latency_ms`, `created_by` |
| `audit_log` | The audit trail | `action`, `outcome`, `username`, `entity_*`, `details` (jsonb), `ip_address` |

Schema and seed live in `backend/src/main/resources/db/migration` (`V1__init_schema.sql`,
`V2__seed_reference_ranges.sql`). Seed accounts are created by a `CommandLineRunner` so passwords
are hashed at runtime from configuration.

## Key flows

**Ingestion (every `poll-interval-ms`):** `LabResultPoller` → `IngestionService.poll()` fetches a
batch from the device as raw JSON (so a single malformed message can't fail the batch). Each
message is handled by `MessageIngestor.ingest()` in its **own transaction**:
parse → Bean-Validation → idempotency check (`external_id`) → map → `AbnormalityEvaluator` flags
each analyte from the reference catalog → PII encrypted by the JPA converter → persist. Outcome is
`STORED` / `SKIPPED_DUPLICATE` / `REJECTED`. A poll summary is written to `audit_log`.

**Abnormality flagging:** `ReferenceRangeCatalog` selects the most specific applicable range
(sex-specific beats sex-agnostic). `AbnormalityEvaluator` applies critical thresholds *before* the
normal range, so a value is `CRITICAL_*` before merely `LOW/HIGH`; missing value or range → `UNKNOWN`.

**Auth:** `POST /api/auth/login` verifies BCrypt and issues an HS256 JWT carrying username + role.
`JwtAuthenticationFilter` authenticates subsequent requests; `SecurityConfig` is stateless, enforces
roles (`/api/audit/**` is ADMIN-only) and returns JSON `401/403`.

**LLM interpretation:** `LlmInterpretationService` builds a **de-identified** prompt
(`PromptBuilder`), calls Ollama `/api/chat` (no DB transaction held during the slow call), persists
the attempt (SUCCESS/FAILURE/TIMEOUT), caches the latest success, and audits `LLM_INTERPRET`.
Failures surface as `503`.

## Resilience

The mock injects abnormal/critical/partial/malformed payloads, duplicate deliveries, empty batches,
artificial latency and transient `500`s. The backend tolerates all of them: malformed → `REJECTED`,
duplicates → skipped, transport failures → logged and retried with the cursor unchanged.
