import Chip from '@mui/material/Chip';
import type { AnalyteFlag } from '../api/types';

const CONFIG: Record<AnalyteFlag, { label: string; color: 'success' | 'warning' | 'error' | 'default' }> = {
  NORMAL: { label: 'Normal', color: 'success' },
  LOW: { label: 'Low', color: 'warning' },
  HIGH: { label: 'High', color: 'warning' },
  CRITICAL_LOW: { label: 'Critical Low', color: 'error' },
  CRITICAL_HIGH: { label: 'Critical High', color: 'error' },
  UNKNOWN: { label: 'N/A', color: 'default' },
};

export function FlagChip({ flag }: { flag: AnalyteFlag }) {
  const { label, color } = CONFIG[flag];
  const critical = flag === 'CRITICAL_LOW' || flag === 'CRITICAL_HIGH';
  return <Chip size="small" label={label} color={color} variant={color === 'default' ? 'outlined' : 'filled'} sx={critical ? { fontWeight: 700 } : undefined} />;
}
