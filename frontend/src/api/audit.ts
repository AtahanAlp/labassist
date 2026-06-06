import { api } from './client';
import type { AuditEntry, PagedResponse } from './types';

export const listAudit = (page = 0, size = 50) =>
  api.get<PagedResponse<AuditEntry>>('/api/audit', { params: { page, size } }).then((r) => r.data);
