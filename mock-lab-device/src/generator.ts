import { ANALYTES, Analyte, PANELS, PANEL_NAMES } from './domain/catalog';
import { DeviceMessage, Scenario, TestItem } from './domain/types';
import { Rng } from './rng';

const FIRST_NAMES = ['Atahan', 'Mehmet', 'Ayse', 'Fatma', 'Mustafa', 'Elif', 'Emir', 'Zeynep', 'Can', 'Deniz', 'Kerem', 'Yusuf', 'Aylin', 'Hakan', 'Ece', 'Burak', 'Selin', 'Onur', 'Merve', 'Cem'];
const LAST_NAMES = ['Yilmaz', 'Demir', 'Kaya', 'Sahin', 'Aydin', 'Koc', 'Arslan', 'Celik', 'Yildiz', 'Yildirim', 'Ozturk', 'Dogan', 'Kilic', 'Aslan', 'Cetin', 'Kara', 'Korkmaz', 'Polat', 'Ozdemir', 'Sen'];

/** Weighted scenario mix for the background (live) emission stream. */
const SCENARIO_WEIGHTS: Array<[Scenario, number]> = [
  ['normal', 0.55],
  ['abnormal', 0.25],
  ['critical', 0.08],
  ['partial', 0.07],
  ['malformed', 0.05],
];

export class Generator {
  private counter = 0;

  constructor(private readonly rng: Rng, private readonly deviceId: string) {}

  /** Pick a scenario according to the weighted live-stream mix. */
  pickScenario(): Scenario {
    const roll = this.rng.next();
    let acc = 0;
    for (const [scenario, weight] of SCENARIO_WEIGHTS) {
      acc += weight;
      if (roll < acc) return scenario;
    }
    return 'normal';
  }

  /** Produce one message. Malformed scenarios return a deliberately broken object. */
  generate(scenario: Scenario): unknown {
    return scenario === 'malformed' ? this.malformed() : this.wellFormed(scenario);
  }

  // ---- well-formed reports -------------------------------------------------

  private wellFormed(scenario: Scenario): DeviceMessage {
    const codes = this.selectCodes(scenario);
    const tests = codes.map((code) => this.measure(ANALYTES[code], scenario, codes));
    this.applyScenarioMutations(scenario, tests, codes);

    const now = new Date();
    const collected = new Date(now.getTime() - this.rng.int(5, 240) * 60_000);
    return {
      externalId: this.nextExternalId(),
      deviceId: this.deviceId,
      scenario,
      patient: this.patient(),
      sampleCollectedAt: collected.toISOString(),
      emittedAt: now.toISOString(),
      tests,
    };
  }

  /** A baseline in-range measurement; scenario mutations are applied afterwards. */
  private measure(a: Analyte, _scenario: Scenario, _codes: string[]): TestItem {
    return {
      code: a.code,
      name: a.name,
      value: this.valueInRange(a),
      unit: a.unit,
      refLow: a.refLow,
      refHigh: a.refHigh,
    };
  }

  private applyScenarioMutations(scenario: Scenario, tests: TestItem[], _codes: string[]): void {
    if (scenario === 'normal') return;

    if (scenario === 'partial') {
      // Device couldn't measure 1–2 analytes → null values.
      const victims = this.rng.shuffle(tests).slice(0, this.rng.int(1, Math.min(2, tests.length)));
      victims.forEach((t) => (t.value = null));
      return;
    }

    if (scenario === 'abnormal') {
      const victims = this.rng.shuffle(tests).slice(0, this.rng.int(1, Math.min(3, tests.length)));
      victims.forEach((t) => (t.value = this.mildlyAbnormal(ANALYTES[t.code])));
      return;
    }

    if (scenario === 'critical') {
      // One or two mild abnormals plus at least one genuinely critical value.
      const criticalCandidates = tests.filter((t) => {
        const a = ANALYTES[t.code];
        return a.criticalLow !== undefined || a.criticalHigh !== undefined;
      });
      const target = this.rng.pick(criticalCandidates.length ? criticalCandidates : tests);
      target.value = this.criticalValue(ANALYTES[target.code]);
      const others = this.rng.shuffle(tests.filter((t) => t !== target)).slice(0, this.rng.int(0, 2));
      others.forEach((t) => (t.value = this.mildlyAbnormal(ANALYTES[t.code])));
    }
  }

  private selectCodes(scenario: Scenario): string[] {
    // `critical` always includes BMP so a critical-capable analyte is present.
    const panels = scenario === 'critical' ? ['BMP'] : [];
    const extra = this.rng.shuffle(PANEL_NAMES).slice(0, this.rng.int(1, 2));
    const chosen = new Set([...panels, ...extra]);
    const codes: string[] = [];
    for (const panel of chosen) {
      for (const code of PANELS[panel]) if (!codes.includes(code)) codes.push(code);
    }
    return codes;
  }

  // ---- value helpers -------------------------------------------------------

  private span(a: Analyte): number {
    return a.refHigh - a.refLow;
  }

  private round(value: number, decimals: number): number {
    const f = 10 ** decimals;
    // Clamp to >= 0: no analyte in the catalog can be physiologically negative.
    return Math.max(0, Math.round(value * f) / f);
  }

  private valueInRange(a: Analyte): number {
    const pad = this.span(a) * 0.05;
    return this.round(this.rng.float(a.refLow + pad, a.refHigh - pad), a.decimals);
  }

  private mildlyAbnormal(a: Analyte): number {
    const span = this.span(a);
    const high = this.rng.chance(0.5);
    if (high) {
      const ceiling = a.criticalHigh !== undefined ? Math.min(a.criticalHigh, a.refHigh + span * 0.6) : a.refHigh + span * 0.6;
      return this.round(this.rng.float(a.refHigh + span * 0.05, Math.max(a.refHigh + span * 0.06, ceiling)), a.decimals);
    }
    const floor = a.criticalLow !== undefined ? Math.max(a.criticalLow, a.refLow - span * 0.6) : a.refLow - span * 0.6;
    return this.round(this.rng.float(Math.min(a.refLow - span * 0.06, floor), a.refLow - span * 0.05), a.decimals);
  }

  private criticalValue(a: Analyte): number {
    const canHigh = a.criticalHigh !== undefined;
    const canLow = a.criticalLow !== undefined;
    const high = canHigh && (!canLow || this.rng.chance(0.6));
    if (high) {
      const c = a.criticalHigh as number;
      return this.round(this.rng.float(c * 1.05, c * 1.35), a.decimals);
    }
    const c = a.criticalLow as number;
    return this.round(this.rng.float(c * 0.45, c * 0.92), a.decimals);
  }

  // ---- malformed payloads --------------------------------------------------

  /** Returns one of several deliberately broken payloads to exercise validation. */
  private malformed(): unknown {
    const base = this.wellFormed('normal') as unknown as Record<string, unknown> & { tests: TestItem[] };
    const style = this.rng.int(0, 5);
    switch (style) {
      case 0: // missing externalId
        delete base.externalId;
        return base;
      case 1: // missing patient block
        delete (base as Record<string, unknown>).patient;
        return base;
      case 2: // a test value as a non-numeric string
        if (base.tests[0]) (base.tests[0] as unknown as Record<string, unknown>).value = 'N/A';
        return base;
      case 3: // tests is not an array
        return { ...base, tests: null };
      case 4: // age as a nonsense string
        (base.patient as unknown as Record<string, unknown>).age = 'unknown';
        return base;
      default: // missing sampleCollectedAt
        delete (base as Record<string, unknown>).sampleCollectedAt;
        return base;
    }
  }

  // ---- identity / patient --------------------------------------------------

  private nextExternalId(): string {
    this.counter += 1;
    return `MSG-${String(this.counter).padStart(6, '0')}`;
  }

  private patient() {
    return {
      name: `${this.rng.pick(FIRST_NAMES)} ${this.rng.pick(LAST_NAMES)}`,
      mrn: `MRN-${this.rng.int(100000, 999999)}`,
      age: this.rng.int(18, 92),
      sex: this.rng.chance(0.5) ? ('M' as const) : ('F' as const),
    };
  }
}
