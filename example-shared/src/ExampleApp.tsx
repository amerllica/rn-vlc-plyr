import { useState } from 'react';
import { StatusBar, StyleSheet } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { LibraryScreen } from './library/LibraryScreen';
import type { PlayerRoute } from './media';
import { PlayerScreen } from './player/PlayerScreen';
import { colors } from './theme';

export function ExampleApp() {
  const [route, setRoute] = useState<PlayerRoute>();

  return (
    <SafeAreaProvider style={styles.root}>
      <StatusBar barStyle="light-content" />
      {route ? (
        <PlayerScreen route={route} onBack={() => setRoute(undefined)} />
      ) : (
        <LibraryScreen onOpen={setRoute} />
      )}
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
    backgroundColor: colors.background,
  },
});
