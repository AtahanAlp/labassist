import { PooledMessage } from './domain/types';

/**
 * Append-only, in-memory pool of emitted messages with monotonic sequence
 * numbers. The backend polls with a `since` cursor and receives everything newer.
 */
export class ResultPool {
  private items: PooledMessage[] = [];
  private seq = 0;
  /** Keep memory bounded; lagging cursors beyond this window simply miss old data. */
  private readonly maxItems = 1000;

  append(message: unknown): PooledMessage {
    this.seq += 1;
    const entry: PooledMessage = { seq: this.seq, message };
    this.items.push(entry);
    if (this.items.length > this.maxItems) this.items.shift();
    return entry;
  }

  /** Everything with seq > cursor, plus the cursor the caller should send next. */
  since(cursor: number): { cursor: number; results: unknown[] } {
    const fresh = this.items.filter((i) => i.seq > cursor);
    const newCursor = fresh.length ? fresh[fresh.length - 1].seq : cursor;
    return { cursor: newCursor, results: fresh.map((i) => i.message) };
  }

  /** A previously emitted well-formed message, used to simulate duplicate delivery. */
  randomWellFormed(pickIndex: (n: number) => number): unknown | undefined {
    const candidates = this.items.filter((i) => {
      const m = i.message as { externalId?: unknown };
      return m && typeof m === 'object' && typeof m.externalId === 'string';
    });
    if (!candidates.length) return undefined;
    return candidates[pickIndex(candidates.length)].message;
  }

  size(): number {
    return this.items.length;
  }
}
