import Chip from '@mui/material/Chip';
import type { ReportStatus } from '../api/types';

const CONFIG: Record<ReportStatus, { color: 'success' | 'warning' | 'error' }> = {
  VALIDATED: { color: 'success' },
  PARTIAL: { color: 'warning' },
  REJECTED: { color: 'error' },
};

export function StatusChip({ status }: { status: ReportStatus }) {
  return <Chip size="small" variant="outlined" label={status} color={CONFIG[status].color} />;
}
