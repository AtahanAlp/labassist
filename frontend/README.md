# frontend

The LabAssist doctor dashboard — React 18 + Vite + TypeScript + MUI.

- **Auth:** JWT stored in `localStorage`, Axios bearer + 401 interceptor, protected routes.
- **Server state:** TanStack Query.
- **Pages:** login, reports list (MUI DataGrid, server pagination, filters, abnormal/critical row
  highlighting), report detail (analytes + flags), AI interpretation panel, admin audit log.

```bash
npm install
npm run dev        # http://localhost:5173 (expects the backend on :8080)
npm run build      # type-check + production build
```

Config: `VITE_API_BASE_URL` (default `http://localhost:8080`). See the root
[`README.md`](../README.md) for the full system and the Docker quick start.
