package net.bdew.pressure.blocks.router;

import java.util.Locale;

/** The legacy six-state router mode, kept per face. */
public enum RouterSideMode {
    DISABLED,
    INPUT_PASSIVE,
    INPUT_ACTIVE,
    OUTPUT_LOW,
    OUTPUT_MEDIUM,
    OUTPUT_HIGH;

    public RouterSideMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return "pressure.router.mode." + name().toLowerCase(Locale.ROOT);
    }

    public boolean acceptsInput() {
        return this == INPUT_PASSIVE || this == INPUT_ACTIVE;
    }

    public boolean outputs() {
        return this == OUTPUT_LOW || this == OUTPUT_MEDIUM || this == OUTPUT_HIGH;
    }
}
