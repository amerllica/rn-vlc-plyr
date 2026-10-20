import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { VlcNowPlaying, VlcPlayer, VlcPlayerEvent } from 'rn-vlc-plyr';
import { LockScreenIcon, NextTrackIcon, VolumeIcon } from '../icons';
import { colors, spacing, typography } from '../theme';
import { Chip } from '../ui/Chip';
import { SectionCard } from '../ui/SectionCard';
import { ToggleRow } from '../ui/ToggleRow';
import { REMOTE_SKIP_INTERVALS_MS } from './options';
import { RemoteTrackListeners } from './RemoteTrackListeners';
import type { PlayerSettings, UpdatePlayerSetting } from './usePlayerSettings';

interface BackgroundSectionProps {
  player: VlcPlayer;
  nowPlaying: VlcNowPlaying;
  settings: PlayerSettings;
  onUpdateSetting: UpdatePlayerSetting;
  onRemoteEvent: (event: VlcPlayerEvent, payload: unknown) => void;
}

const MS_PER_SECOND = 1000;

export function BackgroundSection({
  player,
  nowPlaying,
  settings,
  onUpdateSetting,
  onRemoteEvent,
}: BackgroundSectionProps) {
  const [trackButtons, setTrackButtons] = useState(false);
  const systemControls = settings.nowPlaying !== undefined;

  return (
    <SectionCard
      title="Background"
      icon={<LockScreenIcon size={16} color={colors.accent} />}
    >
      <ToggleRow
        label="Keep playing in background"
        hint="Audio continues on the lock screen. Video pauses until you return"
        icon={<VolumeIcon size={18} color={colors.textSecondary} />}
        value={settings.staysActiveInBackground}
        onChange={(value) => onUpdateSetting('staysActiveInBackground', value)}
      />
      <ToggleRow
        label="System media controls"
        hint="Lock screen, Control Center and the notification shade"
        icon={<LockScreenIcon size={18} color={colors.textSecondary} />}
        value={systemControls}
        onChange={(value) =>
          onUpdateSetting('nowPlaying', value ? nowPlaying : undefined)
        }
      />
      <ToggleRow
        label="Next and previous buttons"
        hint="Shown only while the app listens for them"
        icon={<NextTrackIcon size={18} color={colors.textSecondary} />}
        value={trackButtons}
        onChange={setTrackButtons}
      />
      {trackButtons ? (
        <RemoteTrackListeners player={player} onEvent={onRemoteEvent} />
      ) : null}
      <View style={styles.group}>
        <Text style={typography.overline}>Skip interval</Text>
        <View style={styles.chips}>
          {REMOTE_SKIP_INTERVALS_MS.map((interval) => (
            <Chip
              key={interval}
              label={`${interval / MS_PER_SECOND} s`}
              selected={settings.remoteSkipInterval === interval}
              onPress={() => onUpdateSetting('remoteSkipInterval', interval)}
            />
          ))}
        </View>
      </View>
    </SectionCard>
  );
}

const styles = StyleSheet.create({
  group: {
    gap: spacing.sm,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
});
