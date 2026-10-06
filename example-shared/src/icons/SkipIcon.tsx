import { Path, Text } from 'react-native-svg';
import { colors } from '../theme';
import { IconFrame, type IconProps } from './IconFrame';

interface SkipIconProps extends IconProps {
  direction: 'back' | 'forward';
}

const ARC = {
  back: { arc: 'M4 12a8 8 0 1 0 2.34-5.66', arrow: 'M4 3.5V8.5h5' },
  forward: { arc: 'M20 12a8 8 0 1 1-2.34-5.66', arrow: 'M20 3.5V8.5h-5' },
} as const;

export function SkipIcon({
  size,
  color = colors.text,
  direction,
}: SkipIconProps) {
  const { arc, arrow } = ARC[direction];
  return (
    <IconFrame size={size} color={color}>
      <Path d={arc} />
      <Path d={arrow} />
      <Text
        x={12}
        y={15.4}
        fill={color}
        stroke="none"
        fontSize={7.5}
        fontWeight="700"
        textAnchor="middle"
      >
        10
      </Text>
    </IconFrame>
  );
}
