import { useState } from 'react';
import { ScrollView, StyleSheet, View } from 'react-native';
import { useSafeAreaInsets } from 'react-native-safe-area-context';
import {
  useVlcPlayer,
  useVlcPlayerStatus,
  type VlcPlayer,
  type VlcResizeMode,
  type VlcSurfaceType,
} from 'rn-vlc-plyr';
import type { PlayerRoute } from '../media';
import { colors, spacing } from '../theme';
import { ApiLabSection } from './ApiLabSection';
import { AudioSection } from './AudioSection';
import { FullscreenPlayer } from './FullscreenPlayer';
import { TIME_UPDATE_INTERVAL_MS } from './options';
import { PlaybackSection } from './PlaybackSection';
import { PlayerHeader } from './PlayerHeader';
import { SubtitleSection } from './SubtitleSection';
import { useEventLog } from './useEventLog';
import { useHardwareBack } from './useHardwareBack';
import { useMediaState } from './useMediaState';
import { usePlayerSettings } from './usePlayerSettings';
import { VideoSection } from './VideoSection';
import { VideoStage } from './VideoStage';

interface PlayerScreenProps {
  route: PlayerRoute;
  onBack: () => void;
}

const configurePlayer = (player: VlcPlayer) => {
  player.timeUpdateInterval = TIME_UPDATE_INTERVAL_MS;
};

export function PlayerScreen({ route, onBack }: PlayerScreenProps) {
  const insets = useSafeAreaInsets();
  const player = useVlcPlayer(route.source, configurePlayer);
  const status = useVlcPlayerStatus(player);
  const media = useMediaState(player);
  const [settings, updateSetting] = usePlayerSettings(player);
  const eventLog = useEventLog(player);
  const [resizeMode, setResizeMode] = useState<VlcResizeMode>('contain');
  const [surfaceType, setSurfaceType] = useState<VlcSurfaceType>('texture');
  const [fullscreen, setFullscreen] = useState(false);

  useHardwareBack(onBack);

  const enterFullscreen = () => setFullscreen(true);
  const reload = () => {
    player.source = route.source;
  };

  return (
    <View style={[styles.screen, { paddingTop: insets.top }]}>
      <PlayerHeader title={route.title} status={status} onBack={onBack} />
      <ScrollView
        contentContainerStyle={[
          styles.content,
          { paddingBottom: insets.bottom + spacing.xxl },
        ]}
        keyboardShouldPersistTaps="handled"
      >
        <VideoStage
          player={player}
          status={status}
          media={media}
          title={route.title}
          resizeMode={resizeMode}
          surfaceType={surfaceType}
          onEnterFullscreen={enterFullscreen}
        />
        <PlaybackSection
          player={player}
          status={status}
          settings={settings}
          onUpdateSetting={updateSetting}
          onReload={reload}
        />
        <AudioSection
          player={player}
          media={media}
          settings={settings}
          onUpdateSetting={updateSetting}
        />
        <SubtitleSection
          player={player}
          media={media}
          settings={settings}
          onUpdateSetting={updateSetting}
        />
        <VideoSection
          player={player}
          resizeMode={resizeMode}
          surfaceType={surfaceType}
          onResizeModeChange={setResizeMode}
          onSurfaceTypeChange={setSurfaceType}
          onEnterFullscreen={enterFullscreen}
        />
        <ApiLabSection
          status={status}
          media={media}
          settings={settings}
          events={eventLog.entries}
          onClearEvents={eventLog.clear}
        />
      </ScrollView>
      <FullscreenPlayer
        player={player}
        status={status}
        media={media}
        title={route.title}
        visible={fullscreen}
        resizeMode={resizeMode}
        surfaceType={surfaceType}
        onClose={() => setFullscreen(false)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  screen: {
    flex: 1,
    backgroundColor: colors.background,
  },
  content: {
    gap: spacing.lg,
    paddingHorizontal: spacing.lg,
    paddingTop: spacing.xs,
  },
});
