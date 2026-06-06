// Turkish display labels for backend enum values + locale-aware formatting.
// Backend stays English (enums/API); these map to what the doctor sees.

import type { AnalyteFlag, ReportStatus, Sex, UserRole } from '../api/types';

export const flagLabel: Record<AnalyteFlag, string> = {
  NORMAL: 'Normal',
  LOW: 'Düşük',
  HIGH: 'Yüksek',
  CRITICAL_LOW: 'Kritik Düşük',
  CRITICAL_HIGH: 'Kritik Yüksek',
  UNKNOWN: 'Yok',
};

export const statusLabel: Record<ReportStatus, string> = {
  VALIDATED: 'Doğrulandı',
  PARTIAL: 'Kısmi',
  REJECTED: 'Reddedildi',
};

export const roleLabel: Record<UserRole, string> = {
  DOCTOR: 'Doktor',
  ADMIN: 'Yönetici',
};

export function sexShort(sex: Sex | null | undefined): string {
  if (sex === 'M') return 'E';
  if (sex === 'F') return 'K';
  return '?';
}

export function outcomeLabel(outcome: string): string {
  if (outcome === 'SUCCESS') return 'Başarılı';
  if (outcome === 'FAILURE') return 'Başarısız';
  return outcome;
}

export function formatDateTime(iso: string | null | undefined): string {
  return iso ? new Date(iso).toLocaleString('tr-TR') : '-';
}
