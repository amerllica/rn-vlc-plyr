import type { ReactNode } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import { colors, radii, spacing, typography } from '../theme';

interface SectionCardProps {
  title: string;
  icon?: ReactNode;
  trailing?: ReactNode;
  children?: ReactNode;
}

export function SectionCard({
  title,
  icon,
  trailing,
  children,
}: SectionCardProps) {
  return (
    <View style={styles.card}>
      <View style={styles.header}>
        {icon ? <View style={styles.icon}>{icon}</View> : null}
        <Text style={[typography.headline, styles.title]}>{title}</Text>
        {trailing}
      </View>
      {children ? <View style={styles.body}>{children}</View> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  card: {
    backgroundColor: colors.surface,
    borderRadius: radii.lg,
    borderWidth: StyleSheet.hairlineWidth,
    borderColor: colors.border,
    padding: spacing.lg,
  },
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  icon: {
    width: 32,
    height: 32,
    borderRadius: radii.sm,
    backgroundColor: colors.surfaceRaised,
    alignItems: 'center',
    justifyContent: 'center',
  },
  title: {
    flex: 1,
  },
  body: {
    marginTop: spacing.lg,
    gap: spacing.lg,
  },
});
