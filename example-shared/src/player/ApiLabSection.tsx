import { useState } from 'react';
import { Pressable, StyleSheet, View } from 'react-native';
import { formatVlcTime, type VlcStatus } from 'rn-vlc-plyr';
import { ChevronIcon, CodeIcon } from '../icons';
import { colors, spacing } from '../theme';
import { SectionCard } from '../ui/SectionCard';
import { ValueRow } from '../ui/ValueRow';
import { EventLog } from './EventLog';
import { trackName } from './playback';
import type { EventLogEntry } from './useEventLog';
import type { MediaState } from './useMediaState';
import type { PlayerSettings } from './usePlayerSettings';

interface ApiLabSectionProps {
  status: VlcStatus;
  media: MediaState;
  settings: PlayerSettings;
  events: EventLogEntry[];
  onClearEvents: () => void;
}

const formatMs = (ms: number) => `${formatVlcTime(ms)} (${ms} ms)`;

const formatTrack = (name: string, id: number) =>
  id === -1 ? '-1 (off)' : `${id} (${name})`;

export function ApiLabSection({
  status,
  media,
  settings,
  events,
  onClearEvents,
}: ApiLabSectionProps) {
  const [expanded, setExpanded] = useState(false);
  const values: [string, string, boolean?][] = [
    ['status', status, true],
    ['currentTime', formatMs(media.currentTime)],
    ['duration', formatMs(media.duration)],
    ['rate', String(settings.rate)],
    ['volume', String(media.volume)],
    ['muted', String(media.muted)],
    ['loop', String(settings.loop)],
    ['autoPlay', String(settings.autoPlay)],
    ['isLive', String(media.isLive)],
    ['isSeekable', String(media.isSeekable)],
    ['videoSize', `${media.videoSize.width}×${media.videoSize.height}`],
    [
      'selectedAudioTrack',
      formatTrack(
        trackName(media.audioTracks, media.selectedAudioTrack),
        media.selectedAudioTrack
      ),
    ],
    [
      'selectedSubtitleTrack',
      formatTrack(
        trackName(media.subtitleTracks, media.selectedSubtitleTrack),
        media.selectedSubtitleTrack
      ),
    ],
    [
      'error',
      media.error ? `${media.error.code}: ${media.error.message}` : '—',
      media.error !== undefined,
    ],
  ];

  return (
    <SectionCard
      title="API lab"
      icon={<CodeIcon size={16} color={colors.accent} />}
      trailing={
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={expanded ? 'Collapse API lab' : 'Expand API lab'}
          accessibilityState={{ expanded }}
          onPress={() => setExpanded((value) => !value)}
          style={styles.toggle}
        >
          <ChevronIcon
            size={20}
            color={colors.textSecondary}
            direction={expanded ? 'up' : 'down'}
          />
        </Pressable>
      }
    >
      {expanded ? (
        <>
          <View>
            {values.map(([name, value, highlight]) => (
              <ValueRow
                key={name}
                name={name}
                value={value}
                highlight={highlight}
              />
            ))}
          </View>
          <EventLog entries={events} onClear={onClearEvents} />
        </>
      ) : null}
    </SectionCard>
  );
}

const styles = StyleSheet.create({
  toggle: {
    padding: spacing.xs,
  },
});
