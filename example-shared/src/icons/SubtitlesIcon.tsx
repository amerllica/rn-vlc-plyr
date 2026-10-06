import { Path, Rect } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function SubtitlesIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Rect x={3} y={5} width={18} height={14} rx={3} />
      <Path d="M7 12.5h3" />
      <Path d="M13 12.5h4" />
      <Path d="M7 15.5h6" />
    </IconFrame>
  );
}
