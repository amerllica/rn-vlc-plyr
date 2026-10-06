import { StyleSheet, View } from 'react-native';
import { Forward10Icon, PauseIcon, PlayIcon, Rewind10Icon } from '../icons';
import { colors, spacing } from '../theme';
import { IconButton } from '../ui/IconButton';

interface CenterControlsProps {
  playing: boolean;
  canSkip: boolean;
  large: boolean;
  onTogglePlay: () => void;
  onSkip: (deltaMs: number) => void;
  skipMs: number;
}

export function CenterControls({
  playing,
  canSkip,
  large,
  onTogglePlay,
  onSkip,
  skipMs,
}: CenterControlsProps) {
  const mainSize = large ? 76 : 60;
  const sideSize = large ? 52 : 44;
  const MainIcon = playing ? PauseIcon : PlayIcon;

  return (
    <View style={[styles.row, large && styles.rowLarge]}>
      <IconButton
        label="Back 10 seconds"
        variant="overlay"
        size={sideSize}
        disabled={!canSkip}
        onPress={() => onSkip(-skipMs)}
      >
        <Rewind10Icon size={sideSize * 0.55} />
      </IconButton>
      <IconButton
        label={playing ? 'Pause' : 'Play'}
        variant="accent"
        size={mainSize}
        onPress={onTogglePlay}
      >
        <MainIcon size={mainSize * 0.42} color={colors.onAccent} />
      </IconButton>
      <IconButton
        label="Forward 10 seconds"
        variant="overlay"
        size={sideSize}
        disabled={!canSkip}
        onPress={() => onSkip(skipMs)}
      >
        <Forward10Icon size={sideSize * 0.55} />
      </IconButton>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xl,
  },
  rowLarge: {
    gap: spacing.xxl + spacing.lg,
  },
});
