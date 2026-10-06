import { Rect } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function PauseIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Rect x={6} y={4.5} width={3.5} height={15} rx={1} fill={color} />
      <Rect x={14.5} y={4.5} width={3.5} height={15} rx={1} fill={color} />
    </IconFrame>
  );
}
