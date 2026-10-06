import type { ColorValue, ViewProps } from 'react-native';

type Props = ViewProps & {
  color?: ColorValue;
};

export function RnVlcPlyrView(_props: Props): never {
  throw new Error(
    "'rn-vlc-plyr' is only supported on native platforms."
  );
}
