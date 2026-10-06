import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import type { VlcPlayerEvent } from 'rn-vlc-plyr';
import { colors, radii, spacing, typography } from '../theme';
import { EVENT_LOG_LIMIT, type EventLogEntry } from './useEventLog';

interface EventLogProps {
  entries: EventLogEntry[];
  onClear: () => void;
}

const EVENT_COLORS: Record<VlcPlayerEvent, string> = {
  statusChange: colors.accent,
  timeUpdate: colors.textSecondary,
  loaded: colors.success,
  ended: colors.info,
  error: colors.danger,
  volumeChange: colors.info,
  tracksChange: colors.info,
  videoSizeChange: colors.info,
};

export function EventLog({ entries, onClear }: EventLogProps) {
  return (
    <View style={styles.container}>
      <View style={styles.header}>
        <Text style={[typography.overline, styles.title]}>
          Events · last {EVENT_LOG_LIMIT}
        </Text>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Clear event log"
          onPress={onClear}
        >
          <Text style={styles.clear}>Clear</Text>
        </Pressable>
      </View>
      <ScrollView style={styles.log} nestedScrollEnabled>
        {entries.length === 0 ? (
          <Text style={[typography.mono, styles.empty]}>
            Waiting for events…
          </Text>
        ) : (
          entries.map((entry) => (
            <View key={entry.id} style={styles.entry} testID="event-log-entry">
              <Text style={[typography.mono, styles.at]}>{entry.at}</Text>
              <Text style={[typography.mono, styles.detail]}>
                <Text style={{ color: EVENT_COLORS[entry.event] }}>
                  {entry.event}
                </Text>
                {entry.repeat > 1 ? ` ×${entry.repeat}` : ''}
                {entry.detail ? `  ${entry.detail}` : ''}
              </Text>
            </View>
          ))
        )}
      </ScrollView>
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: spacing.sm,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  title: {
    flex: 1,
  },
  clear: {
    color: colors.accent,
    fontSize: 12,
    fontWeight: '600',
  },
  log: {
    maxHeight: 260,
    borderRadius: radii.md,
    backgroundColor: colors.background,
    padding: spacing.sm,
  },
  empty: {
    color: colors.textMuted,
    padding: spacing.sm,
  },
  entry: {
    flexDirection: 'row',
    gap: spacing.sm,
    paddingVertical: 3,
  },
  at: {
    color: colors.textMuted,
    fontSize: 10,
    lineHeight: 16,
  },
  detail: {
    flex: 1,
    fontSize: 11,
    lineHeight: 16,
  },
});
