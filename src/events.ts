import type {
  VlcError,
  VlcListenerSubscription,
  VlcLoadedInfo,
  VlcPlayer,
  VlcStatus,
  VlcTimeUpdate,
  VlcTracksInfo,
  VlcVideoSize,
  VlcVolumeInfo,
} from './specs/VlcPlayer.nitro';

export interface VlcPlayerEventPayloads {
  statusChange: VlcStatus;
  timeUpdate: VlcTimeUpdate;
  loaded: VlcLoadedInfo;
  ended: undefined;
  error: VlcError;
  volumeChange: VlcVolumeInfo;
  tracksChange: VlcTracksInfo;
  videoSizeChange: VlcVideoSize;
}

export type VlcPlayerEvent = keyof VlcPlayerEventPayloads;

export type VlcPlayerListener<E extends VlcPlayerEvent> = (
  payload: VlcPlayerEventPayloads[E]
) => void;

type Subscribers = {
  [E in VlcPlayerEvent]: (
    player: VlcPlayer,
    listener: VlcPlayerListener<E>
  ) => VlcListenerSubscription;
};

const subscribers: Subscribers = {
  statusChange: (player, listener) =>
    player.addOnStatusChangeListener(listener),
  timeUpdate: (player, listener) => player.addOnTimeUpdateListener(listener),
  loaded: (player, listener) => player.addOnLoadedListener(listener),
  ended: (player, listener) =>
    player.addOnEndedListener(() => listener(undefined)),
  error: (player, listener) => player.addOnErrorListener(listener),
  volumeChange: (player, listener) =>
    player.addOnVolumeChangeListener(listener),
  tracksChange: (player, listener) =>
    player.addOnTracksChangeListener(listener),
  videoSizeChange: (player, listener) =>
    player.addOnVideoSizeChangeListener(listener),
};

export function addVlcPlayerListener<E extends VlcPlayerEvent>(
  player: VlcPlayer,
  event: E,
  listener: VlcPlayerListener<E>
): VlcListenerSubscription {
  const subscribe: Subscribers[E] = subscribers[event];
  return subscribe(player, listener);
}
