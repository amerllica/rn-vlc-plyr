import { Platform, StyleSheet, Text, View } from 'react-native';
import type { VlcPlayer, VlcResizeMode, VlcSurfaceType } from 'rn-vlc-plyr';
import { CameraIcon, FullscreenIcon } from '../icons';
import { colors, spacing, typography } from '../theme';
import { ActionButton } from '../ui/ActionButton';
import { SectionCard } from '../ui/SectionCard';
import { SegmentedControl } from '../ui/SegmentedControl';
import { RESIZE_MODES, SURFACE_TYPES } from './options';
import { SnapshotPreview } from './SnapshotPreview';
import { useSnapshot } from './useSnapshot';

interface VideoSectionProps {
  player: VlcPlayer;
  resizeMode: VlcResizeMode;
  surfaceType: VlcSurfaceType;
  onResizeModeChange: (mode: VlcResizeMode) => void;
  onSurfaceTypeChange: (type: VlcSurfaceType) => void;
  onEnterFullscreen: () => void;
}

export function VideoSection({
  player,
  resizeMode,
  surfaceType,
  onResizeModeChange,
  onSurfaceTypeChange,
  onEnterFullscreen,
}: VideoSectionProps) {
  const { snapshot, error, pending, take } = useSnapshot(player);

  return (
    <SectionCard
      title="Video"
      icon={<FullscreenIcon size={16} color={colors.accent} />}
    >
      <View style={styles.group}>
        <Text style={typography.overline}>Resize mode</Text>
        <SegmentedControl
          accessibilityLabel="Resize mode"
          options={RESIZE_MODES}
          value={resizeMode}
          onChange={onResizeModeChange}
        />
      </View>
      {Platform.OS === 'android' ? (
        <View style={styles.group}>
          <Text style={typography.overline}>Surface type</Text>
          <SegmentedControl
            accessibilityLabel="Surface type"
            options={SURFACE_TYPES}
            value={surfaceType}
            onChange={onSurfaceTypeChange}
          />
        </View>
      ) : null}
      <View style={styles.actions}>
        <ActionButton
          label={pending ? 'Capturing…' : 'Snapshot'}
          icon={<CameraIcon size={16} />}
          disabled={pending}
          onPress={take}
        />
        <ActionButton
          label="Fullscreen"
          icon={<FullscreenIcon size={16} />}
          onPress={onEnterFullscreen}
        />
      </View>
      <SnapshotPreview snapshot={snapshot} error={error} />
    </SectionCard>
  );
}

const styles = StyleSheet.create({
  group: {
    gap: spacing.sm,
  },
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
});
