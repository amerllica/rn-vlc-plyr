import { StyleSheet } from 'react-native';
import Svg, { Defs, LinearGradient, Rect, Stop } from 'react-native-svg';
import { colors } from '../theme';

export function OverlayScrim() {
  return (
    <Svg style={StyleSheet.absoluteFill} pointerEvents="none">
      <Defs>
        <LinearGradient id="overlayScrim" x1="0" y1="0" x2="0" y2="1">
          <Stop offset="0" stopColor={colors.black} stopOpacity={0.65} />
          <Stop offset="0.35" stopColor={colors.black} stopOpacity={0.15} />
          <Stop offset="0.6" stopColor={colors.black} stopOpacity={0.15} />
          <Stop offset="1" stopColor={colors.black} stopOpacity={0.8} />
        </LinearGradient>
      </Defs>
      <Rect x="0" y="0" width="100%" height="100%" fill="url(#overlayScrim)" />
    </Svg>
  );
}
