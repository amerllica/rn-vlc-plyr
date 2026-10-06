import { StyleSheet, Text, View } from 'react-native';
import type { VlcPlayer, VlcStatus } from 'rn-vlc-plyr';
import {
  LoopIcon,
  PauseIcon,
  PlayIcon,
  ReloadIcon,
  SpeedIcon,
  StopIcon,
} from '../icons';
import { colors, spacing, typography } from '../theme';
import { ActionButton } from '../ui/ActionButton';
import { Chip } from '../ui/Chip';
import { SectionCard } from '../ui/SectionCard';
import { ToggleRow } from '../ui/ToggleRow';
import { PLAYBACK_RATES } from './options';
import { isActiveStatus, togglePlayback } from './playback';
import type { PlayerSettings, UpdatePlayerSetting } from './usePlayerSettings';

interface PlaybackSectionProps {
  player: VlcPlayer;
  status: VlcStatus;
  settings: PlayerSettings;
  onUpdateSetting: UpdatePlayerSetting;
  onReload: () => void;
}

export function PlaybackSection({
  player,
  status,
  settings,
  onUpdateSetting,
  onReload,
}: PlaybackSectionProps) {
  const playing = isActiveStatus(status);

  return (
    <SectionCard
      title="Playback"
      icon={<PlayIcon size={16} color={colors.accent} />}
    >
      <View style={styles.actions}>
        <ActionButton
          label={playing ? 'Pause' : 'Play'}
          primary
          icon={
            playing ? (
              <PauseIcon size={16} color={colors.onAccent} />
            ) : (
              <PlayIcon size={16} color={colors.onAccent} />
            )
          }
          onPress={() => togglePlayback(player, status)}
        />
        <ActionButton
          label="Stop"
          icon={<StopIcon size={16} />}
          disabled={status === 'idle'}
          onPress={() => player.stop()}
        />
        <ActionButton
          label="Reload"
          icon={<ReloadIcon size={16} />}
          onPress={onReload}
        />
      </View>
      <View style={styles.group}>
        <View style={styles.groupHeader}>
          <SpeedIcon size={16} color={colors.textMuted} />
          <Text style={typography.overline}>Speed</Text>
        </View>
        <View style={styles.chips}>
          {PLAYBACK_RATES.map((rate) => (
            <Chip
              key={rate}
              label={`${rate}×`}
              selected={settings.rate === rate}
              onPress={() => onUpdateSetting('rate', rate)}
            />
          ))}
        </View>
      </View>
      <ToggleRow
        label="Loop"
        hint="Restart from 0 instead of ending"
        icon={<LoopIcon size={18} color={colors.textSecondary} />}
        value={settings.loop}
        onChange={(value) => onUpdateSetting('loop', value)}
      />
      <ToggleRow
        label="Auto play"
        hint="Applies to the next load. Try it with Reload"
        icon={<PlayIcon size={18} color={colors.textSecondary} />}
        value={settings.autoPlay}
        onChange={(value) => onUpdateSetting('autoPlay', value)}
      />
    </SectionCard>
  );
}

const styles = StyleSheet.create({
  actions: {
    flexDirection: 'row',
    gap: spacing.sm,
  },
  group: {
    gap: spacing.sm,
  },
  groupHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.sm,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
});
