import type { ReactNode } from 'react';
import { StyleSheet, Switch, Text, View } from 'react-native';
import { colors, spacing, typography } from '../theme';

interface ToggleRowProps {
  label: string;
  hint?: string;
  icon?: ReactNode;
  value: boolean;
  onChange: (value: boolean) => void;
}

export function ToggleRow({
  label,
  hint,
  icon,
  value,
  onChange,
}: ToggleRowProps) {
  return (
    <View style={styles.row}>
      {icon}
      <View style={styles.text}>
        <Text style={typography.body}>{label}</Text>
        {hint ? <Text style={typography.caption}>{hint}</Text> : null}
      </View>
      <Switch
        accessibilityLabel={label}
        value={value}
        onValueChange={onChange}
        trackColor={{ false: colors.surfaceRaised, true: colors.accent }}
        thumbColor={colors.text}
        ios_backgroundColor={colors.surfaceRaised}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  text: {
    flex: 1,
    gap: 2,
  },
});
