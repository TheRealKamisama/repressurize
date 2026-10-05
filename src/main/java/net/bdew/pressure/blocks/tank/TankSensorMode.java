package net.bdew.pressure.blocks.tank;

/** The legacy tank sensor families. Fluid match uses the sensor's configurator filter. */
public enum TankSensorMode {
    DISABLED,
    TANK_FILL,
    OUTPUT_FLOW,
    FLUID_MATCH;

    public TankSensorMode next() {
        return values()[(ordinal() + 1) % values().length];
    }

    public String translationKey() {
        return switch (this) {
            case DISABLED -> "pressure.sensor.disabled";
            case TANK_FILL -> "pressure.sensor.tank";
            case OUTPUT_FLOW -> "pressure.sensor.flow";
            case FLUID_MATCH -> "pressure.sensor.fluid";
        };
    }
}
