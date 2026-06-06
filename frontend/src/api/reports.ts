import { api } from './client';
import type {
  LabReportDetail,
  LabReportSummary,
  LlmInterpretation,
  PagedResponse,
  ReportStatus,
  ReportSummary,
} from './types';

export interface ReportQuery {
  page?: number;
  size?: number;
  abnormalOnly?: boolean;
  criticalOnly?: boolean;
  status?: ReportStatus;
  q?: string;
  from?: string; // yyyy-MM-dd
  to?: string; // yyyy-MM-dd
  sort?: string;
}

export const listReports = (params: ReportQuery) =>
  api.get<PagedResponse<LabReportSummary>>('/api/lab-reports', { params }).then((r) => r.data);

export const getReportsSummary = () =>
  api.get<ReportSummary>('/api/lab-reports/summary').then((r) => r.data);

export const getReport = (id: string) =>
  api.get<LabReportDetail>(`/api/lab-reports/${id}`).then((r) => r.data);

export const requestInterpretation = (id: string, refresh = false) =>
  api
    .post<LlmInterpretation>(`/api/lab-reports/${id}/interpretation`, null, { params: { refresh } })
    .then((r) => r.data);

export const getInterpretation = (id: string) =>
  api
    .get<LlmInterpretation>(`/api/lab-reports/${id}/interpretation`)
    .then((r) => (r.status === 204 ? null : r.data));
