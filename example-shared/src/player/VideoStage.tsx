import { StyleSheet, View } from 'react-native';
import {
  VlcPlayerView,
  type VlcPlayer,
  type VlcResizeMode,
  type VlcStatus,
  type VlcSurfaceType,
} from 'rn-vlc-plyr';
import { colors, radii } from '../theme';
import { PlayerOverlay } from './PlayerOverlay';
import type { MediaState } from './useMediaState';

interface VideoStageProps {
  player: VlcPlayer;
  status: VlcStatus;
  media: MediaState;
  title: string;
  resizeMode: VlcResizeMode;
  surfaceType: VlcSurfaceType;
  onEnterFullscreen: () => void;
}

export function VideoStage({
  player,
  status,
  media,
  title,
  resizeMode,
  surfaceType,
  onEnterFullscreen,
}: VideoStageProps) {
  return (
    <View style={styles.stage}>
      <VlcPlayerView
        player={player}
        resizeMode={resizeMode}
        surfaceType={surfaceType}
        style={StyleSheet.absoluteFill}
      />
      <PlayerOverlay
        player={player}
        status={status}
        media={media}
        title={title}
        fullscreen={false}
        onToggleFullscreen={onEnterFullscreen}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  stage: {
    aspectRatio: 16 / 9,
    width: '100%',
    borderRadius: radii.lg,
    overflow: 'hidden',
    backgroundColor: colors.black,
  },
});
