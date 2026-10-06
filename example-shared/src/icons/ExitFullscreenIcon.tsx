import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function ExitFullscreenIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M9 4v5H4" />
      <Path d="M15 4v5h5" />
      <Path d="M9 20v-5H4" />
      <Path d="M15 20v-5h5" />
    </IconFrame>
  );
}
