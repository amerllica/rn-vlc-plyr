import { useRef, useState } from 'react';
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

  const [isMuted, setMuted] = useState(false);
  const [isLooped, setLooped] = useState(false);

  const toggleMuted = () => {
    setMuted((m) => !m);
  };
  const toggleLooped = () => {
    setLooped((l) => !l);
  };

  return (
    <View style={styles.container}>
      <View style={styles.playBar}>
        <Button title="pause" onPress={handlePause} />
        <Button title="play" onPress={handlePlay} />
        <Button title="stop" onPress={handleStop} />
      </View>
      <View style={styles.playBar}>
        <Button title="mute" onPress={toggleMuted} />
        <Button title="loop" onPress={toggleLooped} />
      </View>
      <RnVlcPlyr
        ref={videoRef}
        style={styles.box}
        // library props
        url={oldRatioVideoLink}
        muted={isMuted}
        autoPlay={true}
        loop={isLooped}
      />
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
