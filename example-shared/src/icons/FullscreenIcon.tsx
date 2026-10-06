import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function FullscreenIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M4 9V4h5" />
      <Path d="M20 9V4h-5" />
      <Path d="M4 15v5h5" />
      <Path d="M20 15v5h-5" />
    </IconFrame>
  );
}
