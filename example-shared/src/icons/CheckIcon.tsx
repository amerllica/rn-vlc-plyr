import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function CheckIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d="m5 12.5 4.5 4.5L19 7.5" />
    </IconFrame>
  );
}
