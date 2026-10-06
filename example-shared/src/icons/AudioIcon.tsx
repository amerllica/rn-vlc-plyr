import { Circle, Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function AudioIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M9 18V5l11-2v13" />
      <Circle cx={6.5} cy={18} r={2.5} />
      <Circle cx={17.5} cy={16} r={2.5} />
    </IconFrame>
  );
}
