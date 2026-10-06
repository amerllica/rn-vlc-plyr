import { StyleSheet, Text, View } from 'react-native';
import { PlayIcon } from '../icons';
import { colors, radii, spacing, typography } from '../theme';

export function LibraryHeader() {
  return (
    <View style={styles.header}>
      <View style={styles.logo}>
        <PlayIcon size={22} color={colors.onAccent} />
      </View>
      <View style={styles.text}>
        <Text style={typography.display} accessibilityRole="header">
          rn-vlc-plyr
        </Text>
        <Text style={typography.secondary}>VLC for React Native</Text>
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  header: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.lg,
    paddingTop: spacing.xl,
    paddingBottom: spacing.lg,
  },
  logo: {
    width: 52,
    height: 52,
    borderRadius: radii.lg,
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: colors.accent,
  },
  text: {
    flex: 1,
    gap: 2,
  },
});
