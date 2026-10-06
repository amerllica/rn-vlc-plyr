import { StyleSheet, Text, View } from 'react-native';
import type { VlcStatus } from 'rn-vlc-plyr';
import { ChevronIcon } from '../icons';
import { spacing, typography } from '../theme';
import { IconButton } from '../ui/IconButton';
import { StatusPill } from './StatusPill';

interface PlayerHeaderProps {
  title: string;
  status: VlcStatus;
  onBack: () => void;
}

export function PlayerHeader({ title, status, onBack }: PlayerHeaderProps) {
  return (
    <View style={styles.header}>
      <IconButton label="Back" onPress={onBack}>
        <ChevronIcon direction="left" size={22} />
      </IconButton>
      <View style={styles.text}>
        <Text style={typography.title} numberOfLines={1}>
          {title}
        </Text>
        <StatusPill status={status} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    paddingHorizontal: spacing.lg,
    paddingVertical: spacing.md,
  },
  text: {
    flex: 1,
    gap: spacing.xs,
  },
});
