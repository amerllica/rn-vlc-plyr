import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function LinkIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M10 14a4 4 0 0 0 5.66 0l3-3a4 4 0 0 0-5.66-5.66l-1 1" />
      <Path d="M14 10a4 4 0 0 0-5.66 0l-3 3a4 4 0 0 0 5.66 5.66l1-1" />
    </IconFrame>
  );
}
