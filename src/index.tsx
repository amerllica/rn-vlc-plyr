import { useRef, useImperativeHandle } from 'react';
import RnVlcPlyrView, { Commands } from './RnVlcPlyrViewNativeComponent';
import type * as React from 'react';
import type { ViewProps } from 'react-native';

type VLCPlayerRef = React.ElementRef<typeof RnVlcPlyrView>;

export interface RnVlcPlyrHandlers {
  play: () => void;
  pause: () => void;
  stop: () => void;
}

export interface RnVlcPlyrProps extends ViewProps {
  url?: string;
  ref?: React.Ref<RnVlcPlyrHandlers>;
}

const RnVlcPlyr: React.FC<RnVlcPlyrProps> = ({ ref, ...rest }) => {
  const nativeRef = useRef<VLCPlayerRef>(null);

  useImperativeHandle(
    ref,
    () => ({
      play: () => {
        if (nativeRef.current) {
          Commands.play(nativeRef.current);
        }
      },
      pause: () => {
        if (nativeRef.current) {
          Commands.pause(nativeRef.current);
        }
      },
      stop: () => {
        if (nativeRef.current) {
          Commands.stop(nativeRef.current);
        }
      },
    }),
    []
  );

  return <RnVlcPlyrView {...rest} ref={nativeRef} />;
};

export { RnVlcPlyr };
