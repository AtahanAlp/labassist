/** The scenarios the device can simulate. */
export type Scenario =
  | 'normal' // every analyte within reference range
  | 'abnormal' // one or more analytes mildly out of range (high/low)
  | 'critical' // at least one analyte in the life-threatening range
  | 'partial' // some analytes missing values (device couldn't measure)
  | 'malformed'; // structurally broken payload (missing/wrong-typed fields)

export const SCENARIOS: Scenario[] = ['normal', 'abnormal', 'critical', 'partial', 'malformed'];

/** A single analyte measurement as emitted by the device. */
export interface TestItem {
  code: string;
  name: string;
  value: number | null;
  unit: string;
  refLow: number | null;
  refHigh: number | null;
}

export interface Patient {
  name: string;
  mrn: string;
  age: number;
  sex: 'M' | 'F';
}

/** A complete lab report message emitted by the analyzer. */
export interface DeviceMessage {
  externalId: string;
  deviceId: string;
  scenario: Scenario;
  patient: Patient;
  sampleCollectedAt: string;
  emittedAt: string;
  tests: TestItem[];
}

/** Internal pool entry: a message plus its monotonic sequence number. */
export interface PooledMessage {
  seq: number;
  /** `unknown` because malformed messages intentionally violate the shape. */
  message: unknown;
}
