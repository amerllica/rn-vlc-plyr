import { getHostComponent } from 'react-native-nitro-modules';
import type {
  VlcPlayerViewMethods,
  VlcPlayerViewProps,
} from './specs/VlcPlayerView.nitro';

export const VlcPlayerView = getHostComponent<
  VlcPlayerViewProps,
  VlcPlayerViewMethods
>('VlcPlayerView', () =>
  require('rn-vlc-plyr/nitrogen/generated/shared/json/VlcPlayerViewConfig.json')
);
