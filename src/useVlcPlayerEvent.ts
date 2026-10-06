import { useEffect, useRef, useSyncExternalStore } from 'react';
import type { VlcPlayer, VlcStatus } from './specs/VlcPlayer.nitro';
import {
  addVlcPlayerListener,
  type VlcPlayerEvent,
  type VlcPlayerEventPayloads,
  type VlcPlayerListener,
} from './events';

export function useVlcPlayerEvent<E extends VlcPlayerEvent>(
  player: VlcPlayer,
  event: E,
  listener: VlcPlayerListener<E>
): void {
  const latest = useRef(listener);

  useEffect(() => {
    latest.current = listener;
  });

  useEffect(() => {
    const subscription = addVlcPlayerListener(
      player,
      event,
      (payload: VlcPlayerEventPayloads[E]) => latest.current(payload)
    );
    return () => subscription.remove();
  }, [player, event]);
}

export function useVlcPlayerStatus(player: VlcPlayer): VlcStatus {
  return useSyncExternalStore(
    (onChange) => {
      const subscription = player.addOnStatusChangeListener(onChange);
      return () => subscription.remove();
    },
    () => player.status
  );
}
