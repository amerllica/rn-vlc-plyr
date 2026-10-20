import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function NextTrackIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M5 4l10 8-10 8V4z" />
      <Path d="M19 5v14" />
    </IconFrame>
  );
}
