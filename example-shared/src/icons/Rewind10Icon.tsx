import type { IconProps } from './IconFrame';
import { SkipIcon } from './SkipIcon';

export function Rewind10Icon(props: IconProps) {
  return <SkipIcon {...props} direction="back" />;
}
