import Chip from '@mui/material/Chip';
import type { AnalyteFlag } from '../api/types';
import { flagLabel } from '../i18n/labels';

const COLOR: Record<AnalyteFlag, 'success' | 'warning' | 'error' | 'default'> = {
  NORMAL: 'success',
  LOW: 'warning',
  HIGH: 'warning',
  CRITICAL_LOW: 'error',
  CRITICAL_HIGH: 'error',
  UNKNOWN: 'default',
};

export function FlagChip({ flag }: { flag: AnalyteFlag }) {
  const color = COLOR[flag];
  const critical = flag === 'CRITICAL_LOW' || flag === 'CRITICAL_HIGH';
  return (
    <Chip
      size="small"
      label={flagLabel[flag]}
      color={color}
      variant={color === 'default' ? 'outlined' : 'filled'}
      sx={critical ? { fontWeight: 700 } : undefined}
    />
  );
}
