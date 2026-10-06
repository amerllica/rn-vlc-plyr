import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function ReloadIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M20 12a8 8 0 1 1-2.34-5.66" />
      <Path d="M20 4v5h-5" />
    </IconFrame>
  );
}
