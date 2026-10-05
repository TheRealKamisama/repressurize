package net.bdew.pressure.blocks.router;

import java.util.Locale;

/** Per-face redstone gating used by active router inputs and outputs. */
public enum RouterRedstoneMode {
    ALWAYS,
    RS_ON,
    RS_OFF,
    NEVER;

    public RouterRedstoneMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public boolean allows(boolean powered) {
        return switch (this) {
            case ALWAYS -> true;
            case NEVER -> false;
            case RS_ON -> powered;
            case RS_OFF -> !powered;
        };
    }

    public String translationKey() {
        return "pressure.router.redstone." + name().toLowerCase(Locale.ROOT);
    }
}
