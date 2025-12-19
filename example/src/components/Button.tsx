import { ActivityIndicator, Pressable, Text } from 'react-native';
import { createStyle, isNumber, isString, styleJoiner } from '../utils';
import type { FC } from 'react';
import type { PressableProps } from 'react-native';

export interface ButtonProps extends PressableProps {
  label?: string | number;
  isLoading?: boolean;
}

const Button: FC<ButtonProps> = ({
  style,
  label,
  isLoading,
  disabled,
  ...rest
}) => {
  const isDisabled = disabled || isLoading;
  const kids =
    isString(label) || isNumber(label) ? (
      <Text
        style={styleJoiner(styles.label, isDisabled && styles.labelDisabled)}
      >
        {label}
      </Text>
    ) : null;

  return (
    <Pressable
      {...rest}
      disabled={isDisabled}
      style={({ pressed }) =>
        styleJoiner(
          styles.buttonRoot,
          pressed && styles.pressed,
          isDisabled && styles.disabled,
          style
        )
      }
    >
      {isLoading ? <ActivityIndicator size="small" color="white" /> : kids}
    </Pressable>
  );
};

const styles = createStyle({
  buttonRoot: {
    paddingHorizontal: 16,
    paddingVertical: 8,
    backgroundColor: '#256bda',
    borderRadius: 4,
  },
  disabled: {
    backgroundColor: 'gray',
  },
  label: {
    color: 'white',
    fontSize: 14,
    textAlign: 'center',
  },
  labelDisabled: {
    color: 'black',
  },
  pressed: {
    backgroundColor: '#97b6f4',
  },
});

export { Button };
