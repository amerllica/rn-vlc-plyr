import { useState } from 'react';
import { StyleSheet, Text } from 'react-native';
import type { VlcPlayer } from 'rn-vlc-plyr';
import { DEFAULT_SUBTITLE_URI } from '../media';
import { SubtitlesIcon } from '../icons';
import { colors, typography } from '../theme';
import { ActionButton } from '../ui/ActionButton';
import { PickerRow } from '../ui/PickerRow';
import { SectionCard } from '../ui/SectionCard';
import { Stepper } from '../ui/Stepper';
import { TextField } from '../ui/TextField';
import { TrackPickerSheet } from '../ui/TrackPickerSheet';
import { DELAY_STEP_MS } from './options';
import { trackName } from './playback';
import type { MediaState } from './useMediaState';
import type { PlayerSettings, UpdatePlayerSetting } from './usePlayerSettings';
import { errorMessage } from '../errorMessage';

interface SubtitleSectionProps {
  player: VlcPlayer;
  media: MediaState;
  settings: PlayerSettings;
  onUpdateSetting: UpdatePlayerSetting;
}

export function SubtitleSection({
  player,
  media,
  settings,
  onUpdateSetting,
}: SubtitleSectionProps) {
  const [pickerVisible, setPickerVisible] = useState(false);
  const [subtitleUri, setSubtitleUri] = useState(DEFAULT_SUBTITLE_URI);
  const [feedback, setFeedback] = useState<string>();

  const addSubtitle = () => {
    try {
      player.addSubtitle(subtitleUri.trim(), true);
      setFeedback('Added. The track appears once VLC parses it.');
    } catch (reason) {
      setFeedback(errorMessage(reason));
    }
  };

  return (
    <SectionCard
      title="Subtitles"
      icon={<SubtitlesIcon size={16} color={colors.accent} />}
    >
      <PickerRow
        label="Subtitle track"
        value={trackName(media.subtitleTracks, media.selectedSubtitleTrack)}
        icon={<SubtitlesIcon size={18} color={colors.textSecondary} />}
        onPress={() => setPickerVisible(true)}
      />
      <TextField
        label="External subtitle URL"
        value={subtitleUri}
        onChangeText={setSubtitleUri}
        placeholder="https://…/subtitles.srt"
      />
      <ActionButton
        label="Add external subtitle"
        disabled={subtitleUri.trim() === ''}
        onPress={addSubtitle}
      />
      {feedback ? (
        <Text style={[typography.caption, styles.feedback]}>{feedback}</Text>
      ) : null}
      <Stepper
        label="Subtitle delay"
        value={settings.subtitleDelay}
        step={DELAY_STEP_MS}
        unit="ms"
        onChange={(value) => onUpdateSetting('subtitleDelay', value)}
      />
      <TrackPickerSheet
        title="Subtitle track"
        visible={pickerVisible}
        tracks={media.subtitleTracks}
        selectedId={media.selectedSubtitleTrack}
        onSelect={(id) => player.setSubtitleTrack(id)}
        onClose={() => setPickerVisible(false)}
      />
    </SectionCard>
  );
}

const styles = StyleSheet.create({
  feedback: {
    marginTop: -4,
  },
});
