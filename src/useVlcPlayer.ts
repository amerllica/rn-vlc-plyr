import { useEffect, useRef, useState } from 'react';
import { NitroModules } from 'react-native-nitro-modules';
import type { VlcPlayer, VlcSource } from './specs/VlcPlayer.nitro';

export type VlcSourceInput = VlcSource | string | null | undefined;

export type VlcPlayerSetup = (player: VlcPlayer) => void;

const toSource = (input: VlcSourceInput): VlcSource | undefined => {
  if (input == null || input === '') {
    return undefined;
  }
  return typeof input === 'string' ? { uri: input } : input;
};

const sourceKey = (source: VlcSource | undefined): string =>
  source === undefined ? '' : JSON.stringify(source);

export function createVlcPlayer(
  source?: VlcSourceInput,
  setup?: VlcPlayerSetup
): VlcPlayer {
  const player = NitroModules.createHybridObject<VlcPlayer>('VlcPlayer');
  setup?.(player);
  const initialSource = toSource(source);
  if (initialSource !== undefined) {
    player.source = initialSource;
  }
  return player;
}

export function useVlcPlayer(
  source: VlcSourceInput,
  setup?: VlcPlayerSetup
): VlcPlayer {
  const latest = useRef({ source, setup });
  const [player, setPlayer] = useState(() => createVlcPlayer(source, setup));
  const released = useRef(false);
  const key = sourceKey(toSource(source));

  useEffect(() => {
    latest.current = { source, setup };
  });

  useEffect(() => {
    if (released.current) {
      released.current = false;
      setPlayer(createVlcPlayer(latest.current.source, latest.current.setup));
      return;
    }
    return () => {
      released.current = true;
      player.release();
    };
  }, [player]);

  useEffect(() => {
    if (sourceKey(player.source) !== key) {
      player.source = toSource(latest.current.source);
    }
  }, [player, key]);

  return player;
}
