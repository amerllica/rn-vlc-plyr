import { useCallback, useEffect, useState } from 'react';

export const OVERLAY_HIDE_DELAY_MS = 3000;

export function useAutoHide(autoHide: boolean, pinned: boolean) {
  const [visible, setVisible] = useState(true);
  const [revision, setRevision] = useState(0);

  useEffect(() => {
    if (!autoHide || !visible) {
      return;
    }
    const timer = setTimeout(() => setVisible(false), OVERLAY_HIDE_DELAY_MS);
    return () => clearTimeout(timer);
  }, [autoHide, visible, revision]);

  const reveal = useCallback(() => {
    setVisible(true);
    setRevision((value) => value + 1);
  }, []);

  const toggle = useCallback(() => setVisible((value) => !value), []);

  return { visible: visible || pinned, reveal, toggle };
}
