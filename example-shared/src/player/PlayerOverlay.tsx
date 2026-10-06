import {
  ActivityIndicator,
  Pressable,
  StyleSheet,
  Text,
  View,
  useWindowDimensions,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { VlcPlayer, VlcStatus } from 'rn-vlc-plyr';
import { ExitFullscreenIcon, FullscreenIcon } from '../icons';
import { colors, spacing, typography } from '../theme';
import { IconButton } from '../ui/IconButton';
import { CenterControls } from './CenterControls';
import { ErrorPanel } from './ErrorPanel';
import { OverlayScrim } from './OverlayScrim';
import {
  SKIP_MS,
  isActiveStatus,
  isBusyStatus,
  pinsOverlay,
  togglePlayback,
} from './playback';
import { SeekBar } from './SeekBar';
import { useAutoHide } from './useAutoHide';
import type { MediaState } from './useMediaState';

interface PlayerOverlayProps {
  player: VlcPlayer;
  status: VlcStatus;
  media: MediaState;
  title: string;
  fullscreen: boolean;
  onToggleFullscreen: () => void;
}

export function PlayerOverlay({
  player,
  status,
  media,
  title,
  fullscreen,
  onToggleFullscreen,
}: PlayerOverlayProps) {
  const insets = useSafeAreaInsets();
  const { width, height } = useWindowDimensions();
  const landscape = width > height;
  const { visible, reveal, toggle } = useAutoHide(
    status === 'playing',
    pinsOverlay(status)
  );
  const busy = isBusyStatus(status);
  const edge = fullscreen
    ? {
        paddingTop: Math.max(insets.top, spacing.lg),
        paddingBottom: Math.max(insets.bottom, spacing.lg),
        paddingLeft: Math.max(insets.left, spacing.xl),
        paddingRight: Math.max(insets.right, spacing.xl),
      }
    : styles.inlineEdge;

  const togglePlay = () => {
    togglePlayback(player, status);
    reveal();
  };
  const skip = (deltaMs: number) => {
    player.seekBy(deltaMs);
    reveal();
  };
  const ToggleIcon = fullscreen ? ExitFullscreenIcon : FullscreenIcon;

  return (
    <View style={StyleSheet.absoluteFill}>
      <Pressable
        accessibilityLabel="Toggle controls"
        testID="overlay-toggle"
        style={StyleSheet.absoluteFill}
        onPress={toggle}
      />
      {busy ? (
        <View style={styles.center} pointerEvents="none">
          <ActivityIndicator
            accessibilityLabel="Buffering"
            size="large"
            color={colors.accent}
          />
        </View>
      ) : null}
      {visible ? (
        <View style={[styles.chrome, edge]} pointerEvents="box-none">
          <OverlayScrim />
          <View style={styles.topBar} pointerEvents="box-none">
            {fullscreen ? (
              <Text
                style={[typography.headline, styles.title]}
                numberOfLines={1}
              >
                {title}
              </Text>
            ) : (
              <View style={styles.title} />
            )}
            <IconButton
              label={fullscreen ? 'Exit fullscreen' : 'Enter fullscreen'}
              variant="overlay"
              size={fullscreen ? 44 : 36}
              onPress={onToggleFullscreen}
            >
              <ToggleIcon size={fullscreen ? 22 : 18} />
            </IconButton>
          </View>
          <View style={styles.middle} pointerEvents="box-none">
            {status === 'error' ? (
              <ErrorPanel error={media.error} onRetry={() => player.play()} />
            ) : busy ? null : (
              <CenterControls
                playing={isActiveStatus(status)}
                canSkip={media.isSeekable}
                large={fullscreen && landscape}
                skipMs={SKIP_MS}
                onTogglePlay={togglePlay}
                onSkip={skip}
              />
            )}
          </View>
          <SeekBar
            player={player}
            currentTime={media.currentTime}
            duration={media.duration}
            isSeekable={media.isSeekable}
            isLive={media.isLive}
            onInteraction={reveal}
          />
        </View>
      ) : null}
    </View>
  );
}

const styles = StyleSheet.create({
  center: {
    ...StyleSheet.absoluteFill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  chrome: {
    ...StyleSheet.absoluteFill,
    justifyContent: 'space-between',
  },
  inlineEdge: {
    padding: spacing.md,
  },
  topBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  title: {
    flex: 1,
  },
  middle: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
  },
});
