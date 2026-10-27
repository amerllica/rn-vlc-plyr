import { StyleSheet, Text, View } from 'react-native';
import { ReloadIcon, SpeedIcon } from '../icons';
import { colors, spacing, typography } from '../theme';
import { Chip } from '../ui/Chip';
import { SectionCard } from '../ui/SectionCard';
import { ToggleRow } from '../ui/ToggleRow';
import {
  LARGE_NETWORK_CACHE_OPTIONS,
  RECONNECT_ON_RESUME_AFTER_MS,
  RELOAD_ON_STALL_AFTER_MS,
} from './options';
import type {
  PlayerSettingKey,
  PlayerSettings,
  UpdatePlayerSetting,
} from './usePlayerSettings';

interface NetworkSectionProps {
  settings: PlayerSettings;
  onUpdateSetting: UpdatePlayerSetting;
}

type DelaySettingKey = Extract<
  PlayerSettingKey,
  'reconnectOnResumeAfterMs' | 'reloadOnStallAfterMs'
>;

interface DelayChipsProps {
  title: string;
  settingKey: DelaySettingKey;
  delays: readonly number[];
  settings: PlayerSettings;
  onUpdateSetting: UpdatePlayerSetting;
}

const MS_PER_SECOND = 1000;

const delayLabel = (delayMs: number) =>
  delayMs === 0 ? 'Off' : `${delayMs / MS_PER_SECOND} s`;

function DelayChips({
  title,
  settingKey,
  delays,
  settings,
  onUpdateSetting,
}: DelayChipsProps) {
  return (
    <View style={styles.group}>
      <Text style={typography.overline}>{title}</Text>
      <View style={styles.chips}>
        {delays.map((delayMs) => (
          <Chip
            key={delayMs}
            label={delayLabel(delayMs)}
            accessibilityLabel={`${title}: ${delayLabel(delayMs)}`}
            selected={settings[settingKey] === delayMs}
            onPress={() => onUpdateSetting(settingKey, delayMs)}
          />
        ))}
      </View>
    </View>
  );
}

export function NetworkSection({
  settings,
  onUpdateSetting,
}: NetworkSectionProps) {
  const largeCache = settings.libvlcOptions.length > 0;

  return (
    <SectionCard
      title="Network"
      icon={<ReloadIcon size={16} color={colors.accent} />}
    >
      <DelayChips
        title="Reconnect on resume after"
        settingKey="reconnectOnResumeAfterMs"
        delays={RECONNECT_ON_RESUME_AFTER_MS}
        settings={settings}
        onUpdateSetting={onUpdateSetting}
      />
      <DelayChips
        title="Reload on stall after"
        settingKey="reloadOnStallAfterMs"
        delays={RELOAD_ON_STALL_AFTER_MS}
        settings={settings}
        onUpdateSetting={onUpdateSetting}
      />
      <ToggleRow
        label="Large network cache"
        hint="Sets libvlcOptions to --network-caching=3000 and reloads"
        icon={<SpeedIcon size={18} color={colors.textSecondary} />}
        value={largeCache}
        onChange={(value) =>
          onUpdateSetting(
            'libvlcOptions',
            value ? LARGE_NETWORK_CACHE_OPTIONS : []
          )
        }
      />
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
