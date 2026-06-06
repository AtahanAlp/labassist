/**
 * Analyte catalog with realistic adult reference ranges and critical thresholds.
 *
 * The device sends `refLow`/`refHigh` for realism, but the BACKEND owns the
 * canonical reference catalog and recomputes abnormality flags itself — so these
 * values are illustrative, not authoritative.
 */
export interface Analyte {
  code: string;
  name: string;
  unit: string;
  refLow: number;
  refHigh: number;
  /** Critical thresholds — undefined means "no defined critical bound on that side". */
  criticalLow?: number;
  criticalHigh?: number;
  /** Decimal places when rendering the value. */
  decimals: number;
}

export const ANALYTES: Record<string, Analyte> = {
  GLU: { code: 'GLU', name: 'Glucose, fasting', unit: 'mg/dL', refLow: 70, refHigh: 99, criticalLow: 40, criticalHigh: 500, decimals: 0 },
  NA: { code: 'NA', name: 'Sodium', unit: 'mmol/L', refLow: 136, refHigh: 145, criticalLow: 120, criticalHigh: 160, decimals: 0 },
  K: { code: 'K', name: 'Potassium', unit: 'mmol/L', refLow: 3.5, refHigh: 5.1, criticalLow: 2.5, criticalHigh: 6.5, decimals: 1 },
  CL: { code: 'CL', name: 'Chloride', unit: 'mmol/L', refLow: 98, refHigh: 107, decimals: 0 },
  CREA: { code: 'CREA', name: 'Creatinine', unit: 'mg/dL', refLow: 0.7, refHigh: 1.3, criticalHigh: 7.4, decimals: 2 },
  BUN: { code: 'BUN', name: 'Urea Nitrogen', unit: 'mg/dL', refLow: 7, refHigh: 20, criticalHigh: 100, decimals: 0 },
  HGB: { code: 'HGB', name: 'Hemoglobin', unit: 'g/dL', refLow: 12.0, refHigh: 17.5, criticalLow: 7.0, criticalHigh: 20.0, decimals: 1 },
  WBC: { code: 'WBC', name: 'Leukocytes', unit: '10^3/uL', refLow: 4.5, refHigh: 11.0, criticalLow: 1.0, criticalHigh: 30.0, decimals: 1 },
  PLT: { code: 'PLT', name: 'Platelets', unit: '10^3/uL', refLow: 150, refHigh: 400, criticalLow: 20, criticalHigh: 1000, decimals: 0 },
  ALT: { code: 'ALT', name: 'Alanine Aminotransferase', unit: 'U/L', refLow: 7, refHigh: 56, criticalHigh: 1000, decimals: 0 },
  CRP: { code: 'CRP', name: 'C-Reactive Protein', unit: 'mg/L', refLow: 0, refHigh: 5, decimals: 1 },
  TSH: { code: 'TSH', name: 'Thyrotropin', unit: 'mIU/L', refLow: 0.4, refHigh: 4.0, decimals: 2 },
};

/** Named panels a physician might order — a report carries one or two of these. */
export const PANELS: Record<string, string[]> = {
  BMP: ['GLU', 'NA', 'K', 'CL', 'CREA', 'BUN'],
  CBC: ['HGB', 'WBC', 'PLT'],
  LFT: ['ALT'],
  INFLAMMATION: ['CRP'],
  THYROID: ['TSH'],
};

export const PANEL_NAMES = Object.keys(PANELS);
