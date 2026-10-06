import { StyleSheet, Text, View } from 'react-native';
import type { VlcError } from 'rn-vlc-plyr';
import { ReloadIcon } from '../icons';
import { colors, radii, spacing, typography } from '../theme';
import { ActionButton } from '../ui/ActionButton';

interface ErrorPanelProps {
  error: VlcError | undefined;
  onRetry: () => void;
}

export function ErrorPanel({ error, onRetry }: ErrorPanelProps) {
  return (
    <View style={styles.panel} accessibilityRole="alert">
      <Text style={styles.code}>{error?.code ?? 'error'}</Text>
      <Text style={[typography.body, styles.message]}>
        {error?.message ?? 'Playback failed'}
      </Text>
      <View style={styles.action}>
        <ActionButton
          label="Retry"
          primary
          icon={<ReloadIcon size={18} color={colors.onAccent} />}
          onPress={onRetry}
        />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  panel: {
    alignItems: 'center',
    gap: spacing.sm,
    maxWidth: 320,
    padding: spacing.lg,
    borderRadius: radii.lg,
    backgroundColor: 'rgba(11, 11, 15, 0.82)',
  },
  code: {
    color: colors.danger,
    fontSize: 11,
    fontWeight: '800',
    letterSpacing: 1.2,
    textTransform: 'uppercase',
  },
  message: {
    textAlign: 'center',
  },
  action: {
    flexDirection: 'row',
    marginTop: spacing.xs,
  },
});
