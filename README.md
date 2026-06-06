# LabAssist — Lab Results Smart Assistant

A small hospital system that ingests test results from lab devices, lets a doctor review them,
flags abnormal values, and provides an **AI-assisted preliminary interpretation** — with
password login, encryption of patient data, and an audit trail.

> ⚕️ All patient data in this project is **synthetic**, produced by a mock device. The AI
> commentary is a **preliminary, non-diagnostic** decision-support aid and always recommends
> physician review.

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
                                     │   Ollama llama3.2:3b │     │  PostgreSQL  │
                                     └──────────────────────┘     └──────────────┘
```

| Component | Stack | Responsibility |
|---|---|---|
| `mock-lab-device/` | Node/Express + TypeScript | Simulates a lab analyzer; emits JSON results across many scenarios |
| `backend/` | Spring Boot 3 (Java 17, Maven) | Polls, validates, flags, encrypts, stores, serves REST API, calls the LLM, audits |
| `frontend/` | React + Vite + TypeScript + MUI | Doctor login, result list with abnormal highlighting, detail view, AI interpretation |
| `db` | PostgreSQL + Flyway | Persistence + reproducible schema/seed |
| `llm` | Ollama (llama3.2:3b) | Local, CPU-friendly preliminary interpretation |

---

## Quick start (Docker — one command)

> Prerequisites: Docker + Docker Compose. First run downloads the ~2 GB LLM model.

```bash
cp .env.example .env          # adjust secrets if you like
docker compose up --build     # starts postgres, ollama, mock device, backend, frontend
```

Then open the frontend (default <http://localhost:5173>) and log in with the seeded doctor
account (default `doctor` / `Doctor123!` — see `.env`).

_Detailed setup, local-dev workflow, design decisions, and "what we didn't do" are documented
below and in [`docs/`](docs/) — filled in as the build progresses._

## Local development workflow

_(documented in the Docs phase)_

## Design decisions & rationale

_(documented in the Docs phase — see the plan's decision log)_

## What we deliberately did not do (and why)

_(documented in the Docs phase)_

## Repository layout

```
labassist/
├── mock-lab-device/   # Node/Express/TS — lab device simulator
├── backend/           # Spring Boot — ingestion, API, auth, crypto, LLM, audit
├── frontend/          # React + Vite + MUI — doctor dashboard
├── docs/              # architecture, usage guide, screenshots
├── docker-compose.yml
└── .env.example
```
