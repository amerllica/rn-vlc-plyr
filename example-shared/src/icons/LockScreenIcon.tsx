import { Path, Rect } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function LockScreenIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Rect x="5" y="2" width="14" height="20" rx="2" />
      <Path d="M12 18h.01" />
      <Path d="M9 9h6v5H9z" />
      <Path d="M10 9V7.5a2 2 0 0 1 4 0V9" />
    </IconFrame>
  );
}
