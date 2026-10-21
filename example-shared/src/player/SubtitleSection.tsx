import { useState } from 'react';
import type { VlcPlayer } from 'rn-vlc-plyr';
import { DEFAULT_SUBTITLE_URI } from '../media';
import { SubtitlesIcon } from '../icons';
import { colors } from '../theme';
import { PickerRow } from '../ui/PickerRow';
import { SectionCard } from '../ui/SectionCard';
import { Stepper } from '../ui/Stepper';
import { TrackPickerSheet } from '../ui/TrackPickerSheet';
import { ExternalTrackField } from './ExternalTrackField';
import { DELAY_STEP_MS } from './options';
import { SubtitleStyleControls } from './SubtitleStyleControls';
import { trackName } from './playback';
import type { MediaState } from './useMediaState';
import type { PlayerSettings, UpdatePlayerSetting } from './usePlayerSettings';

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
      <ExternalTrackField
        label="External subtitle URL"
        actionLabel="Add external subtitle"
        placeholder="https://…/subtitles.srt"
        initialUri={DEFAULT_SUBTITLE_URI}
        onAdd={(uri) => player.addSubtitle(uri, true)}
      />
      <Stepper
        label="Subtitle delay"
        value={settings.subtitleDelay}
        step={DELAY_STEP_MS}
        unit="ms"
        onChange={(value) => onUpdateSetting('subtitleDelay', value)}
      />
      <SubtitleStyleControls
        style={settings.subtitleStyle}
        onChange={(style) => onUpdateSetting('subtitleStyle', style)}
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
