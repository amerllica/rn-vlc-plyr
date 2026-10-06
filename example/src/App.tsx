import { useState } from 'react';
import {
  Pressable,
  ScrollView,
  StyleSheet,
  Text,
  TextInput,
  View,
} from 'react-native';
import {
  VlcFullscreenModal,
  VlcPlayerView,
  formatVlcTime,
  useVlcPlayer,
  useVlcPlayerEvent,
  useVlcPlayerStatus,
  type VlcPlayer,
  type VlcResizeMode,
} from 'rn-vlc-plyr';

const MEDIA = [
  {
    label: 'MP4',
    uri: 'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4',
  },
  {
    label: 'MKV tracks',
    uri: 'https://github.com/ietf-wg-cellar/matroska-test-files/raw/master/test_files/test5.mkv',
  },
  {
    label: 'HLS',
    uri: 'https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_hls/master.m3u8',
  },
  { label: 'Broken', uri: 'https://example.invalid/missing.mp4' },
];

const RESIZE_MODES: VlcResizeMode[] = [
  'contain',
  'cover',
  'stretch',
  'original',
];

const SUBTITLE_URI =
  'https://raw.githubusercontent.com/andreyvit/subtitle-tools/master/sample.srt';

function Button({ label, onPress }: { label: string; onPress: () => void }) {
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      testID={label}
      onPress={onPress}
      style={styles.button}
    >
      <Text style={styles.buttonText}>{label}</Text>
    </Pressable>
  );
}

function useEventLog(player: VlcPlayer) {
  const [log, setLog] = useState<string[]>([]);
  const push = (line: string) =>
    setLog((lines) => [line, ...lines].slice(0, 12));
  useVlcPlayerEvent(player, 'statusChange', (status) =>
    push(`status ${status}`)
  );
  useVlcPlayerEvent(player, 'loaded', (info) =>
    push(
      `loaded d=${info.duration} seek=${info.isSeekable} live=${info.isLive} ${info.videoSize.width}x${info.videoSize.height} a=${info.audioTracks.length} s=${info.subtitleTracks.length}`
    )
  );
  useVlcPlayerEvent(player, 'ended', () => push('ended'));
  useVlcPlayerEvent(player, 'error', (error) =>
    push(`error ${error.code} ${error.message}`)
  );
  useVlcPlayerEvent(player, 'volumeChange', (event) =>
    push(`volume ${event.volume} muted=${event.muted}`)
  );
  useVlcPlayerEvent(player, 'tracksChange', (event) =>
    push(
      `tracks a=${event.audioTracks.map((t) => `${t.id}:${t.name}`).join('|')} sel=${event.selectedAudioTrack} s=${event.subtitleTracks.map((t) => `${t.id}:${t.name}`).join('|')} sel=${event.selectedSubtitleTrack}`
    )
  );
  useVlcPlayerEvent(player, 'videoSizeChange', (size) =>
    push(`size ${size.width}x${size.height}`)
  );
  return log;
}

export default function App() {
  const [uri, setUri] = useState(MEDIA[0]!.uri);
  const [resizeMode, setResizeMode] = useState<VlcResizeMode>('contain');
  const [fullscreen, setFullscreen] = useState(false);
  const [mounted, setMounted] = useState(true);
  const [time, setTime] = useState({ currentTime: 0, duration: 0 });
  const [snapshot, setSnapshot] = useState('');
  const [, refresh] = useState(0);
  const player = useVlcPlayer(uri, (p) => {
    p.timeUpdateInterval = 500;
  });
  const status = useVlcPlayerStatus(player);
  const log = useEventLog(player);
  useVlcPlayerEvent(player, 'timeUpdate', setTime);

  const cycleTrack = (kind: 'audio' | 'subtitle') => {
    const tracks =
      kind === 'audio' ? player.audioTracks : player.subtitleTracks;
    const selected =
      kind === 'audio'
        ? player.selectedAudioTrack
        : player.selectedSubtitleTrack;
    const ids = [-1, ...tracks.map((track) => track.id)];
    const next = ids[(ids.indexOf(selected) + 1) % ids.length] ?? -1;
    if (kind === 'audio') {
      player.setAudioTrack(next);
    } else {
      player.setSubtitleTrack(next);
    }
  };

  return (
    <View style={styles.root}>
      {mounted ? (
        <VlcPlayerView
          player={player}
          resizeMode={resizeMode}
          style={styles.video}
        />
      ) : (
        <View style={styles.video} />
      )}
      <Text style={styles.state} testID="state">
        {`${status} ${formatVlcTime(time.currentTime)}/${formatVlcTime(time.duration)} vol=${player.volume} muted=${player.muted} rate=${player.rate} loop=${player.loop} auto=${player.autoPlay} mode=${resizeMode} live=${player.isLive} seekable=${player.isSeekable} ${player.videoSize.width}x${player.videoSize.height} subDelay=${player.subtitleDelay} audioDelay=${player.audioDelay}`}
      </Text>
      <ScrollView contentContainerStyle={styles.controls}>
        {MEDIA.map((media) => (
          <Button
            key={media.label}
            label={media.label}
            onPress={() => setUri(media.uri)}
          />
        ))}
        <TextInput
          style={styles.input}
          placeholder="custom url"
          placeholderTextColor="#777"
          onSubmitEditing={(event) => setUri(event.nativeEvent.text)}
        />
        <Button label="play" onPress={() => player.play()} />
        <Button label="pause" onPress={() => player.pause()} />
        <Button label="stop" onPress={() => player.stop()} />
        <Button label="seek 5s" onPress={() => player.seek(5000)} />
        <Button label="+10s" onPress={() => player.seekBy(10000)} />
        <Button label="-10s" onPress={() => player.seekBy(-10000)} />
        <Button label="vol 30" onPress={() => (player.volume = 30)} />
        <Button label="vol 100" onPress={() => (player.volume = 100)} />
        <Button label="mute" onPress={() => (player.muted = !player.muted)} />
        <Button
          label="rate 2"
          onPress={() => (player.rate = player.rate === 2 ? 1 : 2)}
        />
        <Button label="loop" onPress={() => (player.loop = !player.loop)} />
        <Button
          label="autoPlay"
          onPress={() => (player.autoPlay = !player.autoPlay)}
        />
        <Button label="audio track" onPress={() => cycleTrack('audio')} />
        <Button label="sub track" onPress={() => cycleTrack('subtitle')} />
        <Button
          label="add srt"
          onPress={() => player.addSubtitle(SUBTITLE_URI, true)}
        />
        <Button
          label="sub +500"
          onPress={() => (player.subtitleDelay += 500)}
        />
        <Button label="audio +500" onPress={() => (player.audioDelay += 500)} />
        <Button
          label="snapshot"
          onPress={() =>
            player
              .snapshot()
              .then(setSnapshot, (error: Error) =>
                setSnapshot(`failed ${error.message}`)
              )
          }
        />
        <Button
          label="resize"
          onPress={() =>
            setResizeMode(
              (mode) =>
                RESIZE_MODES[
                  (RESIZE_MODES.indexOf(mode) + 1) % RESIZE_MODES.length
                ]!
            )
          }
        />
        <Button label="fullscreen" onPress={() => setFullscreen(true)} />
        <Button
          label="unmount view"
          onPress={() => setMounted((value) => !value)}
        />
        <Button label="clear source" onPress={() => setUri('')} />
        <Button label="refresh" onPress={() => refresh((n) => n + 1)} />
        <Text style={styles.log} testID="snapshot">
          {snapshot}
        </Text>
        {log.map((line, index) => (
          <Text key={index} style={styles.log}>
            {line}
          </Text>
        ))}
      </ScrollView>
      <VlcFullscreenModal
        player={player}
        visible={fullscreen}
        onClose={() => setFullscreen(false)}
      >
        <View style={styles.fullscreenBar}>
          <Button
            label="exit fullscreen"
            onPress={() => setFullscreen(false)}
          />
          <Button label="pause" onPress={() => player.pause()} />
          <Button label="play" onPress={() => player.play()} />
        </View>
      </VlcFullscreenModal>
    </View>
  );
}

const styles = StyleSheet.create({
  root: { flex: 1, backgroundColor: '#111', paddingTop: 60 },
  video: { width: '100%', aspectRatio: 16 / 9, backgroundColor: '#000' },
  state: { color: '#9f9', fontSize: 11, padding: 6 },
  controls: { flexDirection: 'row', flexWrap: 'wrap', padding: 6, gap: 6 },
  button: {
    backgroundColor: '#333',
    paddingHorizontal: 10,
    paddingVertical: 8,
    borderRadius: 6,
  },
  buttonText: { color: '#fff', fontSize: 12 },
  input: {
    color: '#fff',
    borderColor: '#555',
    borderWidth: 1,
    width: '100%',
    padding: 6,
  },
  log: { color: '#ccc', fontSize: 10, width: '100%' },
  fullscreenBar: {
    position: 'absolute',
    top: 40,
    left: 20,
    flexDirection: 'row',
    gap: 8,
  },
});
