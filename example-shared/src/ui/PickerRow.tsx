import type { ReactNode } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import { ChevronIcon } from '../icons';
import { colors, radii, spacing, typography } from '../theme';

interface PickerRowProps {
  label: string;
  value: string;
  icon?: ReactNode;
  onPress: () => void;
}

export function PickerRow({ label, value, icon, onPress }: PickerRowProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={`${label}: ${value}`}
      onPress={onPress}
      style={({ pressed }) => [styles.row, pressed && styles.pressed]}
    >
      {icon}
      <View style={styles.text}>
        <Text style={typography.caption}>{label}</Text>
        <Text style={typography.body} numberOfLines={1}>
          {value}
        </Text>
      </View>
      <ChevronIcon size={18} color={colors.textMuted} />
    </Pressable>
  );
}

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    padding: spacing.md,
    borderRadius: radii.md,
    backgroundColor: colors.surfaceRaised,
  },
  pressed: {
    backgroundColor: colors.surfacePressed,
  },
  text: {
    flex: 1,
    gap: 2,
  },
});
