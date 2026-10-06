import { Pressable, StyleSheet, Text, View } from 'react-native';
import { colors, radii, spacing, typography } from '../theme';
import { IconButton } from './IconButton';

interface StepperProps {
  label: string;
  value: number;
  step: number;
  unit: string;
  onChange: (value: number) => void;
}

const formatSigned = (value: number, unit: string) =>
  `${value > 0 ? '+' : ''}${value} ${unit}`;

export function Stepper({ label, value, step, unit, onChange }: StepperProps) {
  return (
    <View style={styles.row}>
      <View style={styles.text}>
        <Text style={typography.body}>{label}</Text>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={`Reset ${label}`}
          onPress={() => onChange(0)}
          disabled={value === 0}
        >
          <Text style={[styles.reset, value === 0 && styles.resetDisabled]}>
            Reset
          </Text>
        </Pressable>
      </View>
      <View style={styles.controls}>
        <IconButton
          label={`Decrease ${label}`}
          size={36}
          onPress={() => onChange(value - step)}
        >
          <Text style={styles.sign}>−</Text>
        </IconButton>
        <Text style={[styles.value, value !== 0 && styles.valueActive]}>
          {formatSigned(value, unit)}
        </Text>
        <IconButton
          label={`Increase ${label}`}
          size={36}
          onPress={() => onChange(value + step)}
        >
          <Text style={styles.sign}>+</Text>
        </IconButton>
      </View>
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
  reset: {
    color: colors.accent,
    fontSize: 12,
    fontWeight: '600',
  },
  resetDisabled: {
    color: colors.textMuted,
  },
  controls: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
    backgroundColor: colors.background,
    borderRadius: radii.pill,
    padding: 3,
  },
  sign: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '500',
    lineHeight: 22,
  },
  value: {
    minWidth: 76,
    textAlign: 'center',
    color: colors.textSecondary,
    fontSize: 14,
    fontWeight: '600',
    fontVariant: ['tabular-nums'],
  },
  valueActive: {
    color: colors.text,
  },
});
