import { useState } from 'react';
import { Pressable, StyleSheet, Text, View } from 'react-native';
import type { VlcSource } from 'rn-vlc-plyr';
import { ChevronIcon, LinkIcon, PlayIcon } from '../icons';
import { LIVE_BADGE, type PlayerRoute } from '../media';
import { colors, spacing, typography } from '../theme';
import { ActionButton } from '../ui/ActionButton';
import { Badge } from '../ui/Badge';
import { SectionCard } from '../ui/SectionCard';
import { TextField } from '../ui/TextField';

interface CustomUrlCardProps {
  onOpen: (route: PlayerRoute) => void;
}

const CUSTOM_TITLE = 'Custom URL';

const optional = (value: string) => value.trim() || undefined;

const buildSource = (
  uri: string,
  userAgent: string,
  referrer: string
): VlcSource => ({
  uri: uri.trim(),
  title: CUSTOM_TITLE,
  userAgent: optional(userAgent),
  referrer: optional(referrer),
});

export function CustomUrlCard({ onOpen }: CustomUrlCardProps) {
  const [uri, setUri] = useState('');
  const [userAgent, setUserAgent] = useState('');
  const [referrer, setReferrer] = useState('');
  const [advanced, setAdvanced] = useState(false);

  return (
    <SectionCard
      title={CUSTOM_TITLE}
      icon={<LinkIcon size={16} color={colors.accent} />}
      trailing={<Badge label={LIVE_BADGE.label} tone={LIVE_BADGE.tone} />}
    >
      <Text style={typography.secondary}>
        Any http(s), rtsp, rtmp, udp or file URL. VOD or live.
      </Text>
      <TextField
        label="Media URL"
        value={uri}
        onChangeText={setUri}
        placeholder="https://example.com/stream.m3u8"
      />
      <Pressable
        accessibilityRole="button"
        accessibilityState={{ expanded: advanced }}
        accessibilityLabel="HTTP options"
        onPress={() => setAdvanced((value) => !value)}
        style={styles.disclosure}
      >
        <Text style={typography.caption}>HTTP options</Text>
        <ChevronIcon
          size={14}
          color={colors.textSecondary}
          direction={advanced ? 'up' : 'down'}
        />
      </Pressable>
      {advanced ? (
        <View style={styles.advanced}>
          <TextField
            label="User agent"
            value={userAgent}
            onChangeText={setUserAgent}
            placeholder="Optional"
          />
          <TextField
            label="Referrer"
            value={referrer}
            onChangeText={setReferrer}
            placeholder="Optional"
          />
        </View>
      ) : null}
      <ActionButton
        label="Open player"
        primary
        icon={<PlayIcon size={16} color={colors.onAccent} />}
        disabled={uri.trim() === ''}
        onPress={() =>
          onOpen({
            title: CUSTOM_TITLE,
            source: buildSource(uri, userAgent, referrer),
          })
        }
      />
    </SectionCard>
  );
}

const styles = StyleSheet.create({
  disclosure: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: spacing.xs,
    alignSelf: 'flex-start',
  },
  advanced: {
    gap: spacing.md,
  },
});
