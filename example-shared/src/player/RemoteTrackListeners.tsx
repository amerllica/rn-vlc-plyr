import {
  useVlcPlayerEvent,
  type VlcPlayer,
  type VlcPlayerEvent,
} from 'rn-vlc-plyr';

interface RemoteTrackListenersProps {
  player: VlcPlayer;
  onEvent: (event: VlcPlayerEvent, payload: unknown) => void;
}

export function RemoteTrackListeners({
  player,
  onEvent,
}: RemoteTrackListenersProps) {
  useVlcPlayerEvent(player, 'remoteNext', (payload) =>
    onEvent('remoteNext', payload)
  );
  useVlcPlayerEvent(player, 'remotePrevious', (payload) =>
    onEvent('remotePrevious', payload)
  );
  return null;
}
