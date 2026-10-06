import { Image, StyleSheet, Text, View } from 'react-native';
import { colors, radii, spacing, typography } from '../theme';
import type { Snapshot } from './useSnapshot';

interface SnapshotPreviewProps {
  snapshot: Snapshot | undefined;
  error: string | undefined;
}

const clockTime = (date: Date) =>
  date.toLocaleTimeString([], {
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
  });

export function SnapshotPreview({ snapshot, error }: SnapshotPreviewProps) {
  if (error) {
    return <Text style={[typography.caption, styles.error]}>{error}</Text>;
  }
  if (!snapshot) {
    return null;
  }
  return (
    <View style={styles.row}>
      <Image
        accessibilityLabel="Snapshot"
        source={{ uri: snapshot.uri }}
        style={styles.thumbnail}
        resizeMode="cover"
      />
      <View style={styles.text}>
        <Text style={typography.body}>
          Captured at {clockTime(snapshot.takenAt)}
        </Text>
        <Text style={[typography.mono, styles.uri]} numberOfLines={2}>
          {snapshot.uri}
        </Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.sm,
    borderRadius: radii.md,
    backgroundColor: colors.background,
  },
  thumbnail: {
    width: 112,
    aspectRatio: 16 / 9,
    borderRadius: radii.sm,
    backgroundColor: colors.black,
  },
  text: {
    flex: 1,
    gap: spacing.xs,
  },
  uri: {
    color: colors.textMuted,
    fontSize: 10,
  },
  error: {
    color: colors.danger,
  },
});
