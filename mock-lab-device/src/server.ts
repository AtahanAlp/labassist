import express, { Request, Response } from 'express';
import { Config } from './config';
import { Generator } from './generator';
import { Rng } from './rng';
import { ResultPool } from './store';
import { Scenario, SCENARIOS } from './domain/types';

export interface ServerDeps {
  pool: ResultPool;
  /** Generator backing the live poll stream. */
  liveGenerator: Generator;
  config: Config;
}

const delay = (ms: number) => new Promise((resolve) => setTimeout(resolve, ms));

/**
 * Builds the Express app. Two read paths on `/api/lab-results`:
 *  - `?since=<seq>`  → live poll stream from the growing pool (with chaos injection)
 *  - `?scenario=<s>` → deterministic, freshly generated examples (for demos/tests)
 */
export function createServer(deps: ServerDeps): express.Express {
  const { pool, liveGenerator, config } = deps;
  const app = express();
  app.use(express.json());

  app.get('/health', (_req: Request, res: Response) => {
    res.json({ status: 'UP', deviceId: config.deviceId, poolSize: pool.size() });
  });

  app.get('/api/lab-results', async (req: Request, res: Response) => {
    const scenarioParam = req.query.scenario;
    if (typeof scenarioParam === 'string') {
      return handleScenario(req, res, scenarioParam, config);
    }
    return handlePoll(req, res, pool, config);
  });

  app.use((_req: Request, res: Response) => res.status(404).json({ error: 'not found' }));
  return app;
}

/** Deterministic single-scenario fetch — reproducible, never touches the pool/cursor. */
function handleScenario(req: Request, res: Response, scenarioParam: string, config: Config) {
  const scenario = scenarioParam as Scenario;
  if (!SCENARIOS.includes(scenario)) {
    return res.status(400).json({ error: `unknown scenario '${scenarioParam}'`, allowed: SCENARIOS });
  }
  const count = clamp(parseInt(String(req.query.count ?? '1'), 10) || 1, 1, 20);
  // Fresh seeded generator → same scenario request yields the same payload.
  const seed = config.seed ^ hashScenario(scenario);
  const generator = new Generator(new Rng(seed), config.deviceId);
  const results = Array.from({ length: count }, () => generator.generate(scenario));
  return res.json({ mode: 'scenario', scenario, count, results });
}

/** Live polling stream with optional chaos (artificial latency / transient 500). */
async function handlePoll(req: Request, res: Response, pool: ResultPool, config: Config) {
  const since = Math.max(0, parseInt(String(req.query.since ?? '0'), 10) || 0);
  const roll = new Rng((since + pool.size() + 1) >>> 0);

  if (roll.chance(config.chaosErrorRate)) {
    return res.status(500).json({ error: 'analyzer temporarily unavailable' });
  }
  if (roll.chance(config.chaosDelayRate)) {
    await delay(config.chaosDelayMs);
  }

  const { cursor, results } = pool.since(since);
  return res.json({ mode: 'poll', cursor, count: results.length, results });
}

function clamp(n: number, min: number, max: number): number {
  return Math.min(max, Math.max(min, n));
}

function hashScenario(scenario: string): number {
  let h = 0;
  for (let i = 0; i < scenario.length; i++) h = (Math.imul(31, h) + scenario.charCodeAt(i)) | 0;
  return h;
}
