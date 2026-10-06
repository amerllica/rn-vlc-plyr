import { ScrollView, StyleSheet, Text } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import { MEDIA, routeForMedia, type PlayerRoute } from '../media';
import { colors, spacing, typography } from '../theme';
import { CustomUrlCard } from './CustomUrlCard';
import { LibraryHeader } from './LibraryHeader';
import { MediaCard } from './MediaCard';

interface LibraryScreenProps {
  onOpen: (route: PlayerRoute) => void;
}

export function LibraryScreen({ onOpen }: LibraryScreenProps) {
  const insets = useSafeAreaInsets();

  return (
    <ScrollView
      style={styles.screen}
      contentContainerStyle={[
        styles.content,
        {
          paddingTop: insets.top,
          paddingBottom: insets.bottom + spacing.xxl,
          paddingLeft: insets.left + spacing.lg,
          paddingRight: insets.right + spacing.lg,
        },
      ]}
      keyboardShouldPersistTaps="handled"
    >
      <LibraryHeader />
      <Text style={[typography.overline, styles.sectionTitle]}>
        Sample media
      </Text>
      {MEDIA.map((media) => (
        <MediaCard
          key={media.id}
          media={media}
          onPress={() => onOpen(routeForMedia(media))}
        />
      ))}
      <Text style={[typography.overline, styles.sectionTitle]}>
        Bring your own
      </Text>
      <CustomUrlCard onOpen={onOpen} />
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    gap: spacing.md,
  },
  sectionTitle: {
    marginTop: spacing.md,
  },
});
