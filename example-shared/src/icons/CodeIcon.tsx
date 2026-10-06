import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function CodeIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="m8 7-5 5 5 5" />
      <Path d="m16 7 5 5-5 5" />
      <Path d="m13.5 5-3 14" />
    </IconFrame>
  );
}
