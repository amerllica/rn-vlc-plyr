import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function VolumeIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M11 5 6 9H3v6h3l5 4z" fill={color} />
      <Path d="M15.5 8.5a5 5 0 0 1 0 7" />
      <Path d="M18.5 5.5a9 9 0 0 1 0 13" />
    </IconFrame>
  );
}
