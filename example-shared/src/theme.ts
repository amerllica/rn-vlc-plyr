import { Platform, StyleSheet } from 'react-native';

export const colors = {
  background: '#0B0B0F',
  surface: '#16161D',
  surfaceRaised: '#1F1F29',
  surfacePressed: '#272733',
  border: '#2A2A36',
  text: '#F4F4F7',
  textSecondary: '#9A9AA8',
  textMuted: '#6B6B78',
  accent: '#FF8800',
  accentSoft: 'rgba(255, 136, 0, 0.14)',
  onAccent: '#0B0B0F',
  danger: '#FF5A5F',
  dangerSoft: 'rgba(255, 90, 95, 0.14)',
  info: '#7AA7FF',
  infoSoft: 'rgba(122, 167, 255, 0.14)',
  success: '#3DD68C',
  scrim: 'rgba(0, 0, 0, 0.55)',
  overlayButton: 'rgba(22, 22, 29, 0.72)',
  black: '#000000',
} as const;

export const spacing = {
  xs: 4,
  sm: 8,
  md: 12,
  lg: 16,
  xl: 24,
  xxl: 32,
} as const;

export const radii = {
  sm: 8,
  md: 12,
  lg: 16,
  pill: 999,
} as const;

export const hitSlop = { top: 8, bottom: 8, left: 8, right: 8 } as const;

const monospaceFamily = Platform.select({ ios: 'Menlo', default: 'monospace' });

export const typography = StyleSheet.create({
  display: {
    color: colors.text,
    fontSize: 32,
    fontWeight: '800',
    letterSpacing: -0.8,
  },
  title: {
    color: colors.text,
    fontSize: 20,
    fontWeight: '700',
    letterSpacing: -0.3,
  },
  headline: {
    color: colors.text,
    fontSize: 16,
    fontWeight: '600',
  },
  body: {
    color: colors.text,
    fontSize: 15,
    lineHeight: 21,
  },
  secondary: {
    color: colors.textSecondary,
    fontSize: 14,
    lineHeight: 20,
  },
  caption: {
    color: colors.textSecondary,
    fontSize: 12,
    fontWeight: '500',
  },
  overline: {
    color: colors.textMuted,
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 1.2,
    textTransform: 'uppercase',
  },
  mono: {
    color: colors.text,
    fontFamily: monospaceFamily,
    fontSize: 12,
  },
  tabular: {
    fontVariant: ['tabular-nums'],
  },
});
