import { codegenNativeComponent, type ViewProps } from 'react-native';

interface NativeProps extends ViewProps {
  url?: string;
}

export default codegenNativeComponent<NativeProps>('RnVlcPlyrView');
