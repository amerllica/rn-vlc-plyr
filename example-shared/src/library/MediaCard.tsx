import { Pressable, StyleSheet, Text, View } from 'react-native';
import { ChevronIcon } from '../icons';
import type { MediaItem } from '../media';
import { colors, radii, spacing, typography } from '../theme';
import { Badge } from '../ui/Badge';

interface MediaCardProps {
  media: MediaItem;
  onPress: () => void;
}

export function MediaCard({ media, onPress }: MediaCardProps) {
  const isErrorDemo = media.badges.some((badge) => badge.tone === 'danger');

  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`Play ${media.title}`}
      testID={`media-${media.id}`}
      onPress={onPress}
      style={({ pressed }) => [styles.card, pressed && styles.pressed]}
    >
      <View style={[styles.tile, isErrorDemo && styles.tileDanger]}>
        <Text style={[styles.format, isErrorDemo && styles.formatDanger]}>
          {media.format}
        </Text>
      </View>
      <View style={styles.body}>
        <Text style={typography.headline} numberOfLines={1}>
          {media.title}
        </Text>
        <Text style={typography.secondary} numberOfLines={2}>
          {media.description}
        </Text>
        <View style={styles.badges}>
          {media.badges.map((badge) => (
            <Badge key={badge.label} label={badge.label} tone={badge.tone} />
          ))}
        </View>
      </View>
      <ChevronIcon size={18} color={colors.textMuted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  card: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.lg,
    padding: spacing.lg,
    borderRadius: radii.lg,
    backgroundColor: colors.surface,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
  },
  pressed: {
    backgroundColor: colors.surfaceRaised,
  },
  tile: {
    width: 56,
    height: 56,
    borderRadius: radii.md,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.accentSoft,
  },
  tileDanger: {
    backgroundColor: colors.dangerSoft,
  },
  format: {
    color: colors.accent,
    fontSize: 13,
    fontWeight: '800',
    letterSpacing: 0.6,
  },
  formatDanger: {
    color: colors.danger,
  },
  body: {
    flex: 1,
    gap: spacing.xs,
  },
  badges: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
    marginTop: spacing.xs,
  },
});
