import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function CloseIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="M6 6l12 12" />
      <Path d="M18 6 6 18" />
    </IconFrame>
  );
}
