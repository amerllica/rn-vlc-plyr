import type { ReactNode } from 'react';
import { Pressable, StyleSheet } from 'react-native';
import { colors, hitSlop } from '../theme';

type IconButtonVariant = 'surface' | 'overlay' | 'accent' | 'plain';

interface IconButtonProps {
  label: string;
  onPress: () => void;
  children: ReactNode;
  size?: number;
  variant?: IconButtonVariant;
  disabled?: boolean;
}

const BACKGROUNDS: Record<IconButtonVariant, string> = {
  surface: colors.surfaceRaised,
  overlay: colors.overlayButton,
  accent: colors.accent,
  plain: 'transparent',
};

export function IconButton({
  label,
  onPress,
  children,
  size = 40,
  variant = 'surface',
  disabled = false,
}: IconButtonProps) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      accessibilityState={{ disabled }}
      testID={label}
      onPress={onPress}
      disabled={disabled}
      hitSlop={hitSlop}
      style={({ pressed }) => [
        styles.button,
        {
          width: size,
          height: size,
          borderRadius: size / 2,
          backgroundColor: BACKGROUNDS[variant],
        },
        pressed && styles.pressed,
        disabled && styles.disabled,
      ]}
    >
      {children}
    </Pressable>
  );
}

const styles = StyleSheet.create({
  button: {
    alignItems: 'center',
    justifyContent: 'center',
  },
  pressed: {
    opacity: 0.6,
    transform: [{ scale: 0.94 }],
  },
  disabled: {
    opacity: 0.35,
  },
});
