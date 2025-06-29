package com.moulberry.flashback.keyframe.interpolation;

public enum SidedInterpolationType {

    SMOOTH,
    LINEAR,
    EASE,
    HOLD,
    HERMITE,
    ADVANCED; // <--- Added

    private boolean isSpecial() {
        return this == SidedInterpolationType.SMOOTH || this == SidedInterpolationType.HERMITE;
    }

    public static float interpolate(SidedInterpolationType left, SidedInterpolationType right, float amount) {
        // Special handling for ADVANCED: always return amount (identity), since ADVANCED uses custom logic elsewhere
        if (left == SidedInterpolationType.ADVANCED || right == SidedInterpolationType.ADVANCED) {
            return amount;
        }

        if (left.isSpecial()) {
            if (right.isSpecial()) {
                left = SidedInterpolationType.LINEAR;
                right = SidedInterpolationType.LINEAR;
            } else {
                left = right;
            }
        } else if (right.isSpecial()) {
            right = left;
        }

        if (left == SidedInterpolationType.HOLD) {
            return 0.0f;
        }
        if (right == SidedInterpolationType.HOLD) {
            right = left;
        }

        if (left == SidedInterpolationType.LINEAR) {
            if (right == SidedInterpolationType.LINEAR) {
                return amount;
            } else if (right == SidedInterpolationType.EASE) {
                // https://easings.net/#easeOutCubic
                return 1 - (float) Math.pow(1 - amount, 3);
            }
        } else if (left == SidedInterpolationType.EASE) {
            if (right == SidedInterpolationType.LINEAR) {
                // https://easings.net/#easeInCubic
                return (float) Math.pow(amount, 3);
            } else if (right == SidedInterpolationType.EASE) {
                // https://easings.net/#easeInOutCubic
                if (amount < 0.5) {
                    return 4 * amount * amount * amount;
                } else {
                    return 1 - (float) Math.pow(-2 * amount + 2, 3) / 2;
                }
            }
        }

        throw new IllegalArgumentException("Don't know how to interpolate " + left + " and " + right);
    }
}