import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export function PlayIcon({ size, color }: IconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path
        d="M7 4.9v14.2a1 1 0 0 0 1.52.85l11.36-7.1a1 1 0 0 0 0-1.7L8.52 4.05A1 1 0 0 0 7 4.9z"
        fill={color}
      />
    </IconFrame>
  );
}
