import {
  VlcFullscreenModal,
  type VlcPlayer,
  type VlcResizeMode,
  type VlcStatus,
  type VlcSurfaceType,
} from 'rn-vlc-plyr';
import { PlayerOverlay } from './PlayerOverlay';
import type { MediaState } from './useMediaState';

interface FullscreenPlayerProps {
  player: VlcPlayer;
  status: VlcStatus;
  media: MediaState;
  title: string;
  visible: boolean;
  resizeMode: VlcResizeMode;
  surfaceType: VlcSurfaceType;
  onClose: () => void;
}

export function FullscreenPlayer({
  player,
  status,
  media,
  title,
  visible,
  resizeMode,
  surfaceType,
  onClose,
}: FullscreenPlayerProps) {
  return (
    <VlcFullscreenModal
      player={player}
      visible={visible}
      onClose={onClose}
      resizeMode={resizeMode}
      surfaceType={surfaceType}
    >
      <PlayerOverlay
        player={player}
        status={status}
        media={media}
        title={title}
        fullscreen
        onToggleFullscreen={onClose}
      />
    </VlcFullscreenModal>
  );
}
