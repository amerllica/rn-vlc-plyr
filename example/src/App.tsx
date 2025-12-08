import { View, StyleSheet } from 'react-native';
import { RnVlcPlyrView } from 'rn-vlc-plyr';

const oldRatioVideoLink = 'http://10.208.219.215:8793/oldRatio.mp4';
// const newRatioVideoLink = 'http://10.208.219.215:8793/newRatio.mp4';

export default function App() {
  return (
    <View style={styles.container}>
      <RnVlcPlyrView url={oldRatioVideoLink} style={styles.box} />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  box: {
    width: '100%',
    height: '100%',
  },
});
