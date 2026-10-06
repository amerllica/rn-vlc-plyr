import Slider from '@react-native-community/slider';
import { useState } from 'react';
import { StyleSheet, Text, View } from 'react-native';
import type { VlcPlayer } from 'rn-vlc-plyr';
import { AudioIcon, MuteIcon, VolumeIcon } from '../icons';
import { colors, spacing, typography } from '../theme';
import { IconButton } from '../ui/IconButton';
import { PickerRow } from '../ui/PickerRow';
import { SectionCard } from '../ui/SectionCard';
import { Stepper } from '../ui/Stepper';
import { TrackPickerSheet } from '../ui/TrackPickerSheet';
import { DELAY_STEP_MS } from './options';
import { trackName } from './playback';
import type { MediaState } from './useMediaState';
import type { PlayerSettings, UpdatePlayerSetting } from './usePlayerSettings';

interface AudioSectionProps {
  player: VlcPlayer;
  media: MediaState;
  settings: PlayerSettings;
  onUpdateSetting: UpdatePlayerSetting;
}

export function AudioSection({
  player,
  media,
  settings,
  onUpdateSetting,
}: AudioSectionProps) {
  const [pickerVisible, setPickerVisible] = useState(false);
  const VolumeGlyph = media.muted ? MuteIcon : VolumeIcon;

  return (
    <SectionCard
      title="Audio"
      icon={<VolumeIcon size={16} color={colors.accent} />}
    >
      <View style={styles.volume}>
        <IconButton
          label={media.muted ? 'Unmute' : 'Mute'}
          onPress={() => {
            player.muted = !media.muted;
          }}
        >
          <VolumeGlyph
            size={20}
            color={media.muted ? colors.danger : colors.text}
          />
        </IconButton>
        <Slider
          accessibilityLabel="Volume"
          style={styles.slider}
          minimumValue={0}
          maximumValue={100}
          step={1}
          value={media.volume}
          minimumTrackTintColor={colors.accent}
          maximumTrackTintColor={colors.surfaceRaised}
          thumbTintColor={colors.text}
          onValueChange={(value) => {
            player.volume = Math.round(value);
          }}
        />
        <Text style={[styles.volumeValue, typography.tabular]}>
          {media.volume}
        </Text>
      </View>
      <PickerRow
        label="Audio track"
        value={trackName(media.audioTracks, media.selectedAudioTrack)}
        icon={<AudioIcon size={18} color={colors.textSecondary} />}
        onPress={() => setPickerVisible(true)}
      />
      <Stepper
        label="Audio delay"
        value={settings.audioDelay}
        step={DELAY_STEP_MS}
        unit="ms"
        onChange={(value) => onUpdateSetting('audioDelay', value)}
      />
      <TrackPickerSheet
        title="Audio track"
        visible={pickerVisible}
        tracks={media.audioTracks}
        selectedId={media.selectedAudioTrack}
        onSelect={(id) => player.setAudioTrack(id)}
        onClose={() => setPickerVisible(false)}
      />
    </SectionCard>
  );
}

const styles = StyleSheet.create({
  volume: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.md,
  },
  slider: {
    flex: 1,
    height: 36,
  },
  volumeValue: {
    width: 32,
    textAlign: 'right',
    color: colors.textSecondary,
    fontSize: 13,
    fontWeight: '600',
  },
});
