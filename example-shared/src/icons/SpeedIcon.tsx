import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function SpeedIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M4.2 18a9 9 0 1 1 15.6 0" />
      <Path d="m12 14 4-4.5" />
    </IconFrame>
  );
}
