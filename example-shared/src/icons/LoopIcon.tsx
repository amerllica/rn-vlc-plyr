import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function LoopIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="m17 2 4 4-4 4" />
      <Path d="M3 11V9a3 3 0 0 1 3-3h15" />
      <Path d="m7 22-4-4 4-4" />
      <Path d="M21 13v2a3 3 0 0 1-3 3H3" />
    </IconFrame>
  );
}
