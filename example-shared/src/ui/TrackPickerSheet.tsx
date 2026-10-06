import {
  Modal,
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  View,
} from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import type { VlcTrack } from 'rn-vlc-plyr';
import { CheckIcon } from '../icons';
import { colors, radii, spacing, typography } from '../theme';

interface TrackPickerSheetProps {
  title: string;
  visible: boolean;
  tracks: VlcTrack[];
  selectedId: number;
  onSelect: (id: number) => void;
  onClose: () => void;
}

const OFF_TRACK: VlcTrack = { id: -1, name: 'Off' };

export function TrackPickerSheet({
  title,
  visible,
  tracks,
  selectedId,
  onSelect,
  onClose,
}: TrackPickerSheetProps) {
  const insets = useSafeAreaInsets();
  const options = [OFF_TRACK, ...tracks];

  return (
    <Modal
      visible={visible}
      transparent
      animationType="slide"
      onRequestClose={onClose}
      statusBarTranslucent
    >
      <View style={styles.root}>
        <Pressable
          accessibilityLabel="Close track picker"
          style={styles.backdrop}
          onPress={onClose}
        />
        <View
          style={[styles.sheet, { paddingBottom: insets.bottom + spacing.lg }]}
        >
          <View style={styles.handle} />
          <Text style={[typography.title, styles.title]}>{title}</Text>
          <ScrollView style={styles.list}>
            {options.map((track) => {
              const selected = track.id === selectedId;
              return (
                <Pressable
                  key={track.id}
                  accessibilityRole="button"
                  accessibilityState={{ selected }}
                  accessibilityLabel={track.name}
                  onPress={() => {
                    onSelect(track.id);
                    onClose();
                  }}
                  style={({ pressed }) => [
                    styles.option,
                    selected && styles.selected,
                    pressed && styles.pressed,
                  ]}
                >
                  <Text
                    style={[
                      typography.body,
                      styles.optionName,
                      selected && styles.selectedText,
                    ]}
                    numberOfLines={1}
                  >
                    {track.name}
                  </Text>
                  <Text style={typography.caption}>
                    {track.id === -1 ? '' : `#${track.id}`}
                  </Text>
                  {selected ? (
                    <CheckIcon size={18} color={colors.accent} />
                  ) : null}
                </Pressable>
              );
            })}
          </ScrollView>
        </View>
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  backdrop: {
    ...StyleSheet.absoluteFill,
    backgroundColor: colors.scrim,
  },
  sheet: {
    maxHeight: '70%',
    backgroundColor: colors.surface,
    borderTopLeftRadius: radii.lg + 8,
    borderTopRightRadius: radii.lg + 8,
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.sm,
  },
  handle: {
    alignSelf: 'center',
    width: 40,
    height: 4,
    borderRadius: 2,
    backgroundColor: colors.border,
    marginBottom: spacing.lg,
  },
  title: {
    marginBottom: spacing.md,
  },
  list: {
    flexGrow: 0,
  },
  option: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
    paddingHorizontal: spacing.md,
    paddingVertical: spacing.md + 2,
    borderRadius: radii.md,
  },
  selected: {
    backgroundColor: colors.accentSoft,
  },
  pressed: {
    backgroundColor: colors.surfaceRaised,
  },
  optionName: {
    flex: 1,
  },
  selectedText: {
    color: colors.accent,
    fontWeight: '600',
  },
});
