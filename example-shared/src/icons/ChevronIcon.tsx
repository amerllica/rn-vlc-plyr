import { Path } from 'react-native-svg';
import { IconFrame, type IconProps } from './IconFrame';

export type ChevronDirection = 'left' | 'right' | 'up' | 'down';

interface ChevronIconProps extends IconProps {
  direction?: ChevronDirection;
}

const CHEVRON_PATHS: Record<ChevronDirection, string> = {
  left: 'm15 5-7 7 7 7',
  right: 'm9 5 7 7-7 7',
  up: 'm5 15 7-7 7 7',
  down: 'm5 9 7 7 7-7',
};

export function ChevronIcon({
  size,
  color,
  direction = 'right',
}: ChevronIconProps) {
  return (
    <IconFrame size={size} color={color}>
      <Path d={CHEVRON_PATHS[direction]} />
    </IconFrame>
  );
}
