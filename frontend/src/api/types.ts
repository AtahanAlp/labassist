// Types mirroring the backend REST DTOs.

export type ReportStatus = 'VALIDATED' | 'PARTIAL' | 'REJECTED';
export type AnalyteFlag = 'NORMAL' | 'LOW' | 'HIGH' | 'CRITICAL_LOW' | 'CRITICAL_HIGH' | 'UNKNOWN';
export type Sex = 'M' | 'F' | 'UNKNOWN';
export type LlmStatus = 'SUCCESS' | 'FAILURE' | 'TIMEOUT';
export type UserRole = 'DOCTOR' | 'ADMIN';

export interface UserInfo {
  username: string;
  displayName: string;
  role: UserRole;
}

export interface LoginResponse {
  token: string;
  tokenType: string;
  expiresInMinutes: number;
  user: UserInfo;
}

export interface PagedResponse<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface LabReportSummary {
  id: string;
  externalId: string;
  patientName: string | null;
  patientMrn: string | null;
  patientAge: number | null;
  patientSex: Sex | null;
  deviceId: string | null;
  sampleCollectedAt: string | null;
  receivedAt: string;
  status: ReportStatus;
  overallAbnormal: boolean;
  abnormalCount: number;
  criticalCount: number;
}

export interface TestResultView {
  code: string;
  name: string | null;
  value: number | null;
  unit: string | null;
  refLow: number | null;
  refHigh: number | null;
  flag: AnalyteFlag;
}

export interface LabReportDetail extends LabReportSummary {
  rejectionReason: string | null;
  tests: TestResultView[];
}

export interface LlmInterpretation {
  id: string;
  reportId: string;
  model: string;
  status: LlmStatus;
  responseText: string | null;
  latencyMs: number | null;
  createdBy: string | null;
  createdAt: string;
  errorMessage: string | null;
}

export interface AuditEntry {
  id: string;
  at: string;
  username: string | null;
  action: string;
  outcome: string;
  entityType: string | null;
  entityId: string | null;
  details: string | null;
  ipAddress: string | null;
}
