import { useEffect, useRef } from 'react';
import { BackHandler } from 'react-native';

export function useHardwareBack(onBack: () => void) {
  const latest = useRef(onBack);

  useEffect(() => {
    latest.current = onBack;
  });

  useEffect(() => {
    const subscription = BackHandler.addEventListener(
      'hardwareBackPress',
      () => {
        latest.current();
        return true;
      }
    );
    return () => subscription.remove();
  }, []);
}
