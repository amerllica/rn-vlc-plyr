import { useRef, useImperativeHandle } from 'react';
import RnVlcPlyrView, { Commands } from './RnVlcPlyrViewNativeComponent';
import type {
  NativeProps,
  PlayerStateType,
} from './RnVlcPlyrViewNativeComponent';
import { clamp } from './toolkit';
import type * as React from 'react';
import type { ViewProps, NativeSyntheticEvent } from 'react-native';

type VLCPlayerRef = React.ElementRef<typeof RnVlcPlyrView>;
type VoidFunction = () => void;

export interface RnVlcPlyrHandlers {
  play: VoidFunction;
  pause: VoidFunction;
  stop: VoidFunction;
  seek: (time: number) => void;
  setVolume: (volume: number) => void;
}

export interface RnVlcPlyrProps
  extends Omit<NativeProps, 'onStateChange'>,
    ViewProps {
  ref?: React.Ref<RnVlcPlyrHandlers>;
  onStateChange?: (e: NativeSyntheticEvent<{ state: PlayerStateType }>) => void;
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
      seek: (timeMs) => {
        if (nativeRef.current) {
          const safeTimeMs = clamp(0, timeMs, Number.MAX_SAFE_INTEGER);

          Commands.seek(nativeRef.current, safeTimeMs);
        }
      },
      setVolume: (volume) => {
        if (nativeRef.current) {
          const safeVolume = clamp(0, volume, 100);

          Commands.setVolume(nativeRef.current, safeVolume);
        }
      },
    }),
    []
  );

  return <RnVlcPlyrView {...rest} ref={nativeRef} />;
};

export { RnVlcPlyr };
