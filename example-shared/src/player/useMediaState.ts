import { useCallback, useState } from 'react';
import {
  useVlcPlayerEvent,
  type VlcError,
  type VlcPlayer,
  type VlcTrack,
  type VlcVideoSize,
} from 'rn-vlc-plyr';

export interface MediaState {
  currentTime: number;
  duration: number;
  isLive: boolean;
  isSeekable: boolean;
  videoSize: VlcVideoSize;
  volume: number;
  muted: boolean;
  audioTracks: VlcTrack[];
  subtitleTracks: VlcTrack[];
  selectedAudioTrack: number;
  selectedSubtitleTrack: number;
  error: VlcError | undefined;
}

const readMediaState = (player: VlcPlayer): MediaState => ({
  currentTime: player.currentTime,
  duration: player.duration,
  isLive: player.isLive,
  isSeekable: player.isSeekable,
  videoSize: player.videoSize,
  volume: player.volume,
  muted: player.muted,
  audioTracks: player.audioTracks,
  subtitleTracks: player.subtitleTracks,
  selectedAudioTrack: player.selectedAudioTrack,
  selectedSubtitleTrack: player.selectedSubtitleTrack,
  error: player.error,
});

export function useMediaState(player: VlcPlayer): MediaState {
  const [state, setState] = useState(() => readMediaState(player));
  const merge = useCallback(
    (patch: Partial<MediaState>) =>
      setState((previous) => ({ ...previous, ...patch })),
    []
  );

  useVlcPlayerEvent(player, 'statusChange', (status) => {
    if (status === 'opening') {
      setState(readMediaState(player));
    }
  });
  useVlcPlayerEvent(player, 'timeUpdate', merge);
  useVlcPlayerEvent(player, 'loaded', merge);
  useVlcPlayerEvent(player, 'volumeChange', merge);
  useVlcPlayerEvent(player, 'tracksChange', merge);
  useVlcPlayerEvent(player, 'videoSizeChange', (videoSize) =>
    merge({ videoSize })
  );
  useVlcPlayerEvent(player, 'error', (error) => merge({ error }));

  return state;
}
