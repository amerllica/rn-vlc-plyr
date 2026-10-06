import { useCallback, useState } from 'react';
import type { VlcPlayer } from 'rn-vlc-plyr';
import { errorMessage } from '../errorMessage';

export interface Snapshot {
  uri: string;
  takenAt: Date;
}

export function useSnapshot(player: VlcPlayer) {
  const [snapshot, setSnapshot] = useState<Snapshot>();
  const [error, setError] = useState<string>();
  const [pending, setPending] = useState(false);

  const take = useCallback(async () => {
    setPending(true);
    try {
      const uri = await player.snapshot();
      setSnapshot({ uri, takenAt: new Date() });
      setError(undefined);
    } catch (reason) {
      setError(errorMessage(reason));
    } finally {
      setPending(false);
    }
  }, [player]);

  return { snapshot, error, pending, take };
}
