import { getHostComponent } from 'react-native-nitro-modules';
const RnVlcPlyrConfig = require('../nitrogen/generated/shared/json/RnVlcPlyrConfig.json');
import type { RnVlcPlyrMethods, RnVlcPlyrProps } from './RnVlcPlyr.nitro';

export const RnVlcPlyrView = getHostComponent<RnVlcPlyrProps, RnVlcPlyrMethods>(
  'RnVlcPlyr',
  () => RnVlcPlyrConfig
);
