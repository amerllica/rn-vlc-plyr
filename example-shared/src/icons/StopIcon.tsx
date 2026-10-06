import { Rect } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function StopIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Rect x={5.5} y={5.5} width={13} height={13} rx={2} fill={color} />
    </IconFrame>
  );
}
