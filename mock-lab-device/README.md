# mock-lab-device

A small Node/Express + TypeScript service that simulates a lab analyzer emitting
JSON test results. It is the external system the backend polls.

## Endpoints

| Method | Path | Description |
|---|---|---|
| GET | `/health` | Liveness + pool size. |
| GET | `/api/lab-results?since=<seq>` | **Live poll stream.** Returns messages newer than `since` from a growing pool; responds with `{ mode, cursor, count, results }`. May inject latency or a transient `500` (configurable chaos) to exercise backend resilience. |
| GET | `/api/lab-results?scenario=<name>&count=<n>` | **Deterministic mode.** Freshly generated examples of one scenario (does not touch the pool/cursor). Reproducible per scenario — used by demos and backend tests. |

`scenario` ∈ `normal | abnormal | critical | partial | malformed`.

## Scenarios

- **normal** — every analyte within its reference range.
- **abnormal** — 1–3 analytes mildly out of range (high/low).
- **critical** — at least one life-threatening value (e.g. potassium 7.2 mmol/L).
- **partial** — device couldn't measure some analytes (`value: null`).
- **malformed** — structurally broken payloads (missing `externalId`/`patient`, wrong-typed value, non-array `tests`, …) to test validation.
- Live stream also injects **duplicate** deliveries (same `externalId`) and, occasionally, **empty batches** / **HTTP 500** / **latency**.

## Message shape (well-formed)

```json
{
  "externalId": "MSG-000123",
  "deviceId": "ANALYZER-A1",
  "scenario": "abnormal",
  "patient": { "name": "Ada Yilmaz", "mrn": "MRN-481920", "age": 64, "sex": "F" },
  "sampleCollectedAt": "2026-06-06T08:12:00.000Z",
  "emittedAt": "2026-06-06T09:00:00.000Z",
  "tests": [
    { "code": "K", "name": "Potassium", "value": 5.9, "unit": "mmol/L", "refLow": 3.5, "refHigh": 5.1 }
  ]
}
```

The device sends reference ranges for realism, but the **backend recomputes
abnormality flags from its own canonical catalog** — it does not trust the device.

## Run

```bash
npm install
npm run dev          # watch mode (tsx)
# or
npm run build && npm start
```

Configuration is via environment variables (see `src/config.ts`): `MOCK_DEVICE_PORT`,
`MOCK_DEVICE_SEED`, `MOCK_EMIT_INTERVAL_MS`, `MOCK_INITIAL_REPORTS`,
`MOCK_CHAOS_DELAY_RATE`, `MOCK_CHAOS_ERROR_RATE`.
