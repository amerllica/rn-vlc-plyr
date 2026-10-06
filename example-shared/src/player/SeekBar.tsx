import Slider from '@react-native-community/slider';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { formatVlcTime, type VlcPlayer } from 'rn-vlc-plyr';
import { colors, radii, spacing, typography } from '../theme';

interface SeekBarProps {
  player: VlcPlayer;
  currentTime: number;
  duration: number;
  isSeekable: boolean;
  isLive: boolean;
  onInteraction: () => void;
}

export function SeekBar({
  player,
  currentTime,
  duration,
  isSeekable,
  isLive,
  onInteraction,
}: SeekBarProps) {
  const [scrubTime, setScrubTime] = useState<number>();
  const shownTime = scrubTime ?? currentTime;

  return (
    <View style={styles.row}>
      <Text style={[styles.time, typography.tabular]}>
        {formatVlcTime(shownTime)}
      </Text>
      <Slider
        accessibilityLabel="Seek"
        style={styles.slider}
        minimumValue={0}
        maximumValue={Math.max(duration, 1)}
        value={Math.min(shownTime, Math.max(duration, 1))}
        disabled={!isSeekable}
        minimumTrackTintColor={colors.accent}
        maximumTrackTintColor="rgba(255, 255, 255, 0.3)"
        thumbTintColor={isSeekable ? colors.text : 'transparent'}
        onSlidingStart={(value) => {
          setScrubTime(value);
          onInteraction();
        }}
        onValueChange={(value) => {
          if (scrubTime !== undefined) {
            setScrubTime(value);
          }
        }}
        onSlidingComplete={(value) => {
          player.seek(value);
          setScrubTime(undefined);
          onInteraction();
        }}
      />
      {isLive ? (
        <View style={styles.live}>
          <View style={styles.liveDot} />
          <Text style={styles.liveLabel}>LIVE</Text>
        </View>
      ) : (
        <Text style={[styles.time, styles.total, typography.tabular]}>
          {formatVlcTime(duration)}
        </Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  slider: {
    flex: 1,
    height: 32,
  },
  time: {
    minWidth: 40,
    color: colors.text,
    fontSize: 12,
    fontWeight: '600',
  },
  total: {
    color: colors.textSecondary,
    textAlign: 'right',
  },
  live: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    paddingHorizontal: spacing.sm,
    paddingVertical: 3,
    borderRadius: radii.pill,
    backgroundColor: colors.danger,
  },
  liveDot: {
    width: 6,
    height: 6,
    borderRadius: 3,
    backgroundColor: colors.text,
  },
  liveLabel: {
    color: colors.text,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 0.8,
  },
});
