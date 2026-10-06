import type {
  HybridView,
  HybridViewMethods,
  HybridViewProps,
} from 'react-native-nitro-modules';

export interface RnVlcPlyrProps extends HybridViewProps {
  color: string;
}
export interface RnVlcPlyrMethods extends HybridViewMethods {}

export type RnVlcPlyr = HybridView<
  RnVlcPlyrProps,
  RnVlcPlyrMethods
>;
