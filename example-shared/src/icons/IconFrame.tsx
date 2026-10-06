import type { ReactNode } from 'react';
import Svg, { G } from 'react-native-svg';
import { colors } from '../theme';

export interface IconProps {
  size?: number;
  color?: string;
}

interface IconFrameProps extends IconProps {
  children: ReactNode;
}

export function IconFrame({
  size = 24,
  color = colors.text,
  children,
}: IconFrameProps) {
  return (
    <Svg width={size} height={size} viewBox="0 0 24 24" fill="none">
      <G
        stroke={color}
        strokeWidth={2}
        strokeLinecap="round"
        strokeLinejoin="round"
      >
        {children}
      </G>
    </Svg>
  );
}
