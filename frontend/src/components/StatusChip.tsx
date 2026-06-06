import Chip from '@mui/material/Chip';
import type { ReportStatus } from '../api/types';
import { statusLabel } from '../i18n/labels';

const COLOR: Record<ReportStatus, 'success' | 'warning' | 'error'> = {
  VALIDATED: 'success',
  PARTIAL: 'warning',
  REJECTED: 'error',
};

export function StatusChip({ status }: { status: ReportStatus }) {
  return <Chip size="small" variant="outlined" label={statusLabel[status]} color={COLOR[status]} />;
}
