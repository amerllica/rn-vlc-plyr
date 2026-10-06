import { useCallback, useState } from 'react';
import type { VlcPlayer } from 'rn-vlc-plyr';

export type PlayerSettingKey =
  'rate' | 'loop' | 'autoPlay' | 'audioDelay' | 'subtitleDelay';

export type PlayerSettings = Pick<VlcPlayer, PlayerSettingKey>;

export type UpdatePlayerSetting = <K extends PlayerSettingKey>(
  key: K,
  value: VlcPlayer[K]
) => void;

const readSettings = (player: VlcPlayer): PlayerSettings => ({
  rate: player.rate,
  loop: player.loop,
  autoPlay: player.autoPlay,
  audioDelay: player.audioDelay,
  subtitleDelay: player.subtitleDelay,
});

export function usePlayerSettings(
  player: VlcPlayer
): [PlayerSettings, UpdatePlayerSetting] {
  const [settings, setSettings] = useState(() => readSettings(player));

  const update = useCallback<UpdatePlayerSetting>(
    (key, value) => {
      player[key] = value;
      setSettings(readSettings(player));
    },
    [player]
  );

  return [settings, update];
}
