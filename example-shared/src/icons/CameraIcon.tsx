import { Circle, Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function CameraIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M4 8h3l2-3h6l2 3h3a1 1 0 0 1 1 1v10a1 1 0 0 1-1 1H4a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1z" />
      <Circle cx={12} cy={13.5} r={3.5} />
    </IconFrame>
  );
}
