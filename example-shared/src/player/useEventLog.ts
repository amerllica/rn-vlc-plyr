import { useCallback, useRef, useState } from 'react';
import {
  useVlcPlayerEvent,
  type VlcPlayer,
  type VlcPlayerEvent,
} from 'rn-vlc-plyr';

export interface EventLogEntry {
  id: number;
  at: string;
  event: VlcPlayerEvent;
  detail: string;
  repeat: number;
}

export const EVENT_LOG_LIMIT = 30;
const DETAIL_LIMIT = 160;
const COALESCED_EVENTS: ReadonlySet<VlcPlayerEvent> = new Set(['timeUpdate']);

const pad = (value: number, length = 2) => String(value).padStart(length, '0');

const timestamp = (date: Date) =>
  `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(
    date.getSeconds()
  )}.${pad(date.getMilliseconds(), 3)}`;

const describePayload = (payload: unknown): string => {
  if (payload === undefined) {
    return '';
  }
  const text = typeof payload === 'string' ? payload : JSON.stringify(payload);
  return text.length > DETAIL_LIMIT ? `${text.slice(0, DETAIL_LIMIT)}…` : text;
};

const appendEntry = (
  entries: EventLogEntry[],
  entry: EventLogEntry
): EventLogEntry[] => {
  const [latest, ...older] = entries;
  if (latest?.event === entry.event && COALESCED_EVENTS.has(entry.event)) {
    return [{ ...entry, repeat: latest.repeat + 1 }, ...older];
  }
  return [entry, ...entries].slice(0, EVENT_LOG_LIMIT);
};

export function useEventLog(player: VlcPlayer) {
  const [entries, setEntries] = useState<EventLogEntry[]>([]);
  const nextId = useRef(0);

  const record = useCallback((event: VlcPlayerEvent, payload: unknown) => {
    nextId.current += 1;
    const entry: EventLogEntry = {
      id: nextId.current,
      at: timestamp(new Date()),
      event,
      detail: describePayload(payload),
      repeat: 1,
    };
    setEntries((previous) => appendEntry(previous, entry));
  }, []);

  const clear = useCallback(() => setEntries([]), []);

  useVlcPlayerEvent(player, 'statusChange', (payload) =>
    record('statusChange', payload)
  );
  useVlcPlayerEvent(player, 'timeUpdate', (payload) =>
    record('timeUpdate', payload)
  );
  useVlcPlayerEvent(player, 'loaded', (payload) => record('loaded', payload));
  useVlcPlayerEvent(player, 'ended', (payload) => record('ended', payload));
  useVlcPlayerEvent(player, 'error', (payload) => record('error', payload));
  useVlcPlayerEvent(player, 'volumeChange', (payload) =>
    record('volumeChange', payload)
  );
  useVlcPlayerEvent(player, 'tracksChange', (payload) =>
    record('tracksChange', payload)
  );
  useVlcPlayerEvent(player, 'videoSizeChange', (payload) =>
    record('videoSizeChange', payload)
  );

  return { entries, clear };
}
