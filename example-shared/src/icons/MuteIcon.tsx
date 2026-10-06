import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function MuteIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M11 5 6 9H3v6h3l5 4z" fill={color} />
      <Path d="m16 9 6 6" />
      <Path d="m22 9-6 6" />
    </IconFrame>
  );
}
