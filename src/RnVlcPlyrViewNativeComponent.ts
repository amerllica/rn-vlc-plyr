import { codegenNativeCommands, codegenNativeComponent } from 'react-native';
import type * as React from 'react';
import type { ViewProps, HostComponent } from 'react-native';

interface NativeProps extends ViewProps {
  url?: string;
}

type RnVlcPlyrViewType = HostComponent<NativeProps>;

export interface NativeCommands {
  play: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
  pause: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
  stop: (viewRef: React.ElementRef<RnVlcPlyrViewType>) => void;
}

export const Commands = codegenNativeCommands<NativeCommands>({
  supportedCommands: ['play', 'pause', 'stop'],
});

export default codegenNativeComponent<NativeProps>('RnVlcPlyrView');
