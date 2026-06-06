/**
 * Runtime configuration, read from environment with sensible defaults so the
 * service runs locally with zero setup.
 */
export interface Config {
  port: number;
  deviceId: string;
  /** PRNG seed — fixed by default so demo runs are reproducible. */
  seed: number;
  /** Background generator cadence: a new report is emitted every N ms. */
  emitIntervalMs: number;
  /** How many reports to pre-seed into the pool at startup. */
  initialReports: number;
  /** Probability the live poll endpoint injects an artificial delay. */
  chaosDelayRate: number;
  /** Probability the live poll endpoint responds with HTTP 500. */
  chaosErrorRate: number;
  /** Artificial delay duration when chaos delay fires. */
  chaosDelayMs: number;
}

function num(name: string, fallback: number): number {
  const raw = process.env[name];
  if (raw === undefined || raw === '') return fallback;
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : fallback;
}

export const config: Config = {
  port: num('MOCK_DEVICE_PORT', 9090),
  deviceId: process.env.MOCK_DEVICE_ID ?? 'ANALYZER-A1',
  seed: num('MOCK_DEVICE_SEED', 20240601),
  emitIntervalMs: num('MOCK_EMIT_INTERVAL_MS', 4000),
  initialReports: num('MOCK_INITIAL_REPORTS', 8),
  chaosDelayRate: num('MOCK_CHAOS_DELAY_RATE', 0.1),
  chaosErrorRate: num('MOCK_CHAOS_ERROR_RATE', 0.05),
  chaosDelayMs: num('MOCK_CHAOS_DELAY_MS', 2500),
};
