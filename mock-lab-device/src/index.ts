import { config } from './config';
import { Generator } from './generator';
import { Rng } from './rng';
import { createServer } from './server';
import { ResultPool } from './store';
import { Scenario } from './domain/types';

/** Probability the background emitter re-delivers an existing message (duplicate). */
const DUPLICATE_RATE = 0.05;
/** Guarantee an interesting first poll: one of each scenario up front. */
const STARTER_SCENARIOS: Scenario[] = ['normal', 'abnormal', 'critical', 'partial', 'malformed'];

function main(): void {
  const rng = new Rng(config.seed);
  const liveGenerator = new Generator(rng, config.deviceId);
  const control = new Rng(config.seed ^ 0x9e3779b9);
  const pool = new ResultPool();

  // Pre-seed the pool so the backend's first poll already has a varied spread.
  STARTER_SCENARIOS.forEach((s) => pool.append(liveGenerator.generate(s)));
  for (let i = STARTER_SCENARIOS.length; i < config.initialReports; i++) {
    pool.append(liveGenerator.generate(liveGenerator.pickScenario()));
  }
  log(`pre-seeded ${pool.size()} reports`);

  // Background stream: emit a new report every interval, occasionally a duplicate.
  const timer = setInterval(() => {
    if (control.chance(DUPLICATE_RATE)) {
      const dup = pool.randomWellFormed((n) => control.int(0, n - 1));
      if (dup) {
        pool.append(dup);
        log(`emitted DUPLICATE of ${(dup as { externalId?: string }).externalId}`);
        return;
      }
    }
    const scenario = liveGenerator.pickScenario();
    pool.append(liveGenerator.generate(scenario));
    log(`emitted ${scenario} (pool=${pool.size()})`);
  }, config.emitIntervalMs);
  timer.unref?.();

  const app = createServer({ pool, liveGenerator, config });
  const server = app.listen(config.port, () => {
    log(`mock lab device listening on :${config.port} (device ${config.deviceId}, seed ${config.seed})`);
  });

  const shutdown = (signal: string) => {
    log(`received ${signal}, shutting down`);
    clearInterval(timer);
    server.close(() => process.exit(0));
  };
  process.on('SIGINT', () => shutdown('SIGINT'));
  process.on('SIGTERM', () => shutdown('SIGTERM'));
}

function log(message: string): void {
  // eslint-disable-next-line no-console
  console.log(`[${new Date().toISOString()}] [mock-lab-device] ${message}`);
}

main();
