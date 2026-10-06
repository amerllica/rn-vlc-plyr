import type { ReactNode } from 'react';
import { Modal, StatusBar, StyleSheet, View } from 'react-native';
import type { VlcPlayer } from './specs/VlcPlayer.nitro';
import type {
  VlcResizeMode,
  VlcSurfaceType,
} from './specs/VlcPlayerView.nitro';
import { VlcPlayerView } from './VlcPlayerView';

export interface VlcFullscreenModalProps {
  player: VlcPlayer;
  visible: boolean;
  onClose: () => void;
  resizeMode?: VlcResizeMode;
  surfaceType?: VlcSurfaceType;
  backgroundColor?: string;
  children?: ReactNode;
}

const ALL_ORIENTATIONS = [
  'portrait',
  'portrait-upside-down',
  'landscape',
  'landscape-left',
  'landscape-right',
] as const;

export function VlcFullscreenModal({
  player,
  visible,
  onClose,
  resizeMode = 'contain',
  surfaceType,
  backgroundColor = '#000',
  children,
}: VlcFullscreenModalProps) {
  return (
    <Modal
      visible={visible}
      onRequestClose={onClose}
      animationType="fade"
      presentationStyle="fullScreen"
      supportedOrientations={ALL_ORIENTATIONS}
      statusBarTranslucent
      navigationBarTranslucent
    >
      <StatusBar hidden={visible} animated />
      <View style={[styles.root, { backgroundColor }]}>
        {visible ? (
          <VlcPlayerView
            player={player}
            resizeMode={resizeMode}
            surfaceType={surfaceType}
            style={StyleSheet.absoluteFill}
          />
        ) : null}
        {children}
      </View>
    </Modal>
  );
}

const styles = StyleSheet.create({
  root: {
    flex: 1,
  },
});
