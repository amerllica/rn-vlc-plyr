import { StyleSheet, Text, View } from 'react-native';
import type { VlcSubtitleStyle } from 'rn-vlc-plyr';
import { spacing, typography } from '../theme';
import { ActionButton } from '../ui/ActionButton';
import { Chip } from '../ui/Chip';
import { SegmentedControl } from '../ui/SegmentedControl';
import { ToggleRow } from '../ui/ToggleRow';
import {
  DEFAULT_OUTLINE_THICKNESS,
  DEFAULT_SUBTITLE_COLOR,
  DEFAULT_SUBTITLE_FONT_SCALE,
  OUTLINE_THICKNESSES,
  SUBTITLE_BACKGROUND_COLOR,
  SUBTITLE_COLORS,
  SUBTITLE_FONT_SCALES,
} from './options';

interface SubtitleStyleControlsProps {
  style: VlcSubtitleStyle | undefined;
  onChange: (style: VlcSubtitleStyle | undefined) => void;
}

export function SubtitleStyleControls({
  style,
  onChange,
}: SubtitleStyleControlsProps) {
  const current = style ?? {};
  const update = (change: VlcSubtitleStyle) =>
    onChange({ ...current, ...change });
  const fontScale = current.fontScale ?? DEFAULT_SUBTITLE_FONT_SCALE;
  const color = current.color ?? DEFAULT_SUBTITLE_COLOR;

  return (
    <View style={styles.container}>
      <Text style={typography.overline}>Style</Text>
      <Text style={typography.caption}>
        A change reloads the media at the same position.
      </Text>
      <Text style={typography.caption}>Size</Text>
      <View style={styles.chips}>
        {SUBTITLE_FONT_SCALES.map((scale) => (
          <Chip
            key={scale}
            label={`${scale}%`}
            selected={fontScale === scale}
            onPress={() => update({ fontScale: scale })}
          />
        ))}
      </View>
      <Text style={typography.caption}>Colour</Text>
      <View style={styles.chips}>
        {SUBTITLE_COLORS.map((option) => (
          <Chip
            key={option.value}
            label={option.label}
            selected={color === option.value}
            onPress={() => update({ color: option.value })}
          />
        ))}
      </View>
      <Text style={typography.caption}>Outline</Text>
      <SegmentedControl
        accessibilityLabel="Subtitle outline"
        options={OUTLINE_THICKNESSES}
        value={current.outlineThickness ?? DEFAULT_OUTLINE_THICKNESS}
        onChange={(outlineThickness) => update({ outlineThickness })}
      />
      <ToggleRow
        label="Bold subtitles"
        value={current.bold ?? false}
        onChange={(bold) => update({ bold })}
      />
      <ToggleRow
        label="Subtitle background"
        hint="A dark box behind the text"
        value={current.backgroundColor !== undefined}
        onChange={(enabled) =>
          update({
            backgroundColor: enabled ? SUBTITLE_BACKGROUND_COLOR : undefined,
          })
        }
      />
      <ActionButton
        label="Reset subtitle style"
        disabled={style === undefined}
        onPress={() => onChange(undefined)}
      />
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    gap: spacing.sm,
  },
  chips: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: spacing.sm,
  },
});
