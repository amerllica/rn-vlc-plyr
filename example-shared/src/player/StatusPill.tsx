import { StyleSheet, Text, View } from 'react-native';
import type { VlcStatus } from 'rn-vlc-plyr';
import { colors, radii, spacing } from '../theme';

const STATUS_COLORS: Record<VlcStatus, string> = {
  idle: colors.textMuted,
  opening: colors.accent,
  buffering: colors.accent,
  playing: colors.success,
  paused: colors.textSecondary,
  stopped: colors.textMuted,
  ended: colors.info,
  error: colors.danger,
};

export function StatusPill({ status }: { status: VlcStatus }) {
  const color = STATUS_COLORS[status];
  return (
    <View style={styles.pill} testID="status-pill">
      <View style={[styles.dot, { backgroundColor: color }]} />
      <Text style={[styles.label, { color }]}>{status}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  pill: {
    flexDirection: 'row',
    alignItems: 'center',
    alignSelf: 'flex-start',
    gap: 6,
    paddingHorizontal: spacing.sm,
    paddingVertical: 3,
    borderRadius: radii.pill,
    backgroundColor: colors.surfaceRaised,
  },
  dot: {
    width: 6,
    height: 6,
    borderRadius: 3,
  },
  label: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.6,
    textTransform: 'uppercase',
  },
});
