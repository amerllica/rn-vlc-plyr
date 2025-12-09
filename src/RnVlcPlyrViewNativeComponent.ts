import { codegenNativeCommands, codegenNativeComponent } from 'react-native';
import type * as React from 'react';
import type { ViewProps, HostComponent } from 'react-native';
// @ts-expect-error
import type * as CodegenTypes from 'react-native/Libraries/Types/CodegenTypes';

export type PlayerStateType =
  | 'buffering'
  | 'playing'
  | 'paused'
  | 'stopped'
  | 'ended'
  | 'error'
  | 'idle';

export interface NativeProps extends ViewProps {
  // Configs
  url?: string;
  autoPlay?: boolean;
  loop?: boolean;
  muted?: boolean;
  // Events
  onStateChange?: CodegenTypes.DirectEventHandler<{ state: string }>;
  onProgress?: CodegenTypes.DirectEventHandler<{
    currentTime: CodegenTypes.Double;
    duration: CodegenTypes.Double;
  }>;
  onLoad?: CodegenTypes.DirectEventHandler<{
    duration: CodegenTypes.Double;
    width: CodegenTypes.Double;
    height: CodegenTypes.Double;
  }>;
  onError?: CodegenTypes.DirectEventHandler<{
    message: string;
    code: CodegenTypes.Int32;
  }>;
  onVolumeChange?: CodegenTypes.DirectEventHandler<{
    volume: CodegenTypes.Double;
  }>;
}

type RnVlcPlyrViewType = HostComponent<NativeProps>;

export interface NativeCommands {
  play: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
  pause: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
  stop: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
  presentFullscreen: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
  dismissFullscreen: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
  seek: (
    viewRef: React.ElementRef<RnVlcPlyrViewType>,
    time: CodegenTypes.Double
  ) => void;
  setVolume: (
    viewRef: React.ElementRef<RnVlcPlyrViewType>,
    volume: CodegenTypes.Double
  ) => void;
}

export const Commands = codegenNativeCommands<NativeCommands>({
  supportedCommands: [
    'play',
    'pause',
    'stop',
    'presentFullscreen',
    'dismissFullscreen',
    'seek',
    'setVolume',
  ],
});

export default codegenNativeComponent<NativeProps>('RnVlcPlyrView');
