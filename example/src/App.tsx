import { useRef } from 'react';
import { View, StyleSheet, Button } from 'react-native';
import { RnVlcPlyr, type RnVlcPlyrHandlers } from 'rn-vlc-plyr';

const oldRatioVideoLink = 'http://10.208.219.215:8793/oldRatio.mp4';
// const newRatioVideoLink = 'http://10.208.219.215:8793/newRatio.mp4';

export default function App() {
  const videoRef = useRef<RnVlcPlyrHandlers>(null);

  const handlePause = () => {
    videoRef.current?.pause();
  };
  const handlePlay = () => {
    videoRef.current?.play();
  };
  const handleStop = () => {
    videoRef.current?.stop();
  };

  return (
    <View style={styles.container}>
      <View style={styles.playBar}>
        <Button title="pause" onPress={handlePause} />
        <Button title="play" onPress={handlePlay} />
        <Button title="stop" onPress={handleStop} />
      </View>
      <RnVlcPlyr ref={videoRef} url={oldRatioVideoLink} style={styles.box} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    gap: 20,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  playBar: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 20,
  },
  box: {
    width: '100%',
    height: 500,
  },
});
