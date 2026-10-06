import { View, StyleSheet } from 'react-native';
import { VlcPlayerView, useVlcPlayer } from 'rn-vlc-plyr';

const SAMPLE =
  'https://test-videos.co.uk/vids/bigbuckbunny/mp4/h264/1080/Big_Buck_Bunny_1080_10s_1MB.mp4';

export default function App() {
  const player = useVlcPlayer(SAMPLE);
  return (
    <View style={styles.container}>
      <VlcPlayerView player={player} style={styles.video} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    justifyContent: 'center',
    backgroundColor: '#000',
  },
  video: {
    width: '100%',
    aspectRatio: 16 / 9,
  },
});
