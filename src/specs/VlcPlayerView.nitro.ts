import type {
  HybridView,
  HybridViewMethods,
  HybridViewProps,
} from 'react-native-nitro-modules';
import type { VlcPlayer } from './VlcPlayer.nitro';

export type VlcResizeMode = 'contain' | 'cover' | 'stretch' | 'original';

export type VlcSurfaceType = 'texture' | 'surface';

export interface VlcPlayerViewProps extends HybridViewProps {
  player?: VlcPlayer;
  resizeMode?: VlcResizeMode;
  surfaceType?: VlcSurfaceType;
}

export interface VlcPlayerViewMethods extends HybridViewMethods {}

export type VlcPlayerView = HybridView<
  VlcPlayerViewProps,
  VlcPlayerViewMethods
>;
