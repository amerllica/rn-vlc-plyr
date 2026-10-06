import { StyleSheet, Text, View } from 'react-native';
import type { BadgeTone } from '../media';
import { colors, radii, spacing } from '../theme';

interface BadgeProps {
  label: string;
  tone?: BadgeTone;
}

const TONES: Record<BadgeTone, { background: string; foreground: string }> = {
  neutral: {
    background: colors.surfaceRaised,
    foreground: colors.textSecondary,
  },
  accent: { background: colors.accentSoft, foreground: colors.accent },
  info: { background: colors.infoSoft, foreground: colors.info },
  danger: { background: colors.dangerSoft, foreground: colors.danger },
};

export function Badge({ label, tone = 'neutral' }: BadgeProps) {
  const { background, foreground } = TONES[tone];
  return (
    <View style={[styles.badge, { backgroundColor: background }]}>
      <Text style={[styles.label, { color: foreground }]}>{label}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  badge: {
    paddingHorizontal: spacing.sm,
    paddingVertical: 3,
    borderRadius: radii.pill,
  },
  label: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.3,
  },
});
