import { StyleSheet, Text, View } from 'react-native';
import { colors, spacing, typography } from '../theme';

interface ValueRowProps {
  name: string;
  value: string;
  highlight?: boolean;
}

export function ValueRow({ name, value, highlight = false }: ValueRowProps) {
  return (
    <View style={styles.row}>
      <Text style={[typography.mono, styles.name]}>{name}</Text>
      <Text
        style={[typography.mono, styles.value, highlight && styles.highlight]}
        numberOfLines={2}
        testID={`api-${name}`}
      >
        {value}
      </Text>
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    gap: spacing.md,
    paddingVertical: spacing.xs + 1,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: colors.border,
  },
  name: {
    width: 150,
    color: colors.textSecondary,
  },
  value: {
    flex: 1,
    textAlign: 'right',
  },
  highlight: {
    color: colors.accent,
  },
});
